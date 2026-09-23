import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.0";
import { resolveIdentity } from "../_shared/identity.ts";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type, x-dev-profile-id, x-dev-user-role",
};

serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });

  try {
    const supabaseClient = createClient(
      Deno.env.get("SUPABASE_URL") ?? "",
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? ""
    );

    const identity = await resolveIdentity(req, supabaseClient);
    if (identity.isAdmin) {
      return new Response(JSON.stringify({ error: "Forbidden: Admin cannot ack receipts" }), {
        status: 403,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    const { conversation_id, read_seq } = await req.json();
    if (!conversation_id || typeof read_seq !== "number") {
      return new Response(JSON.stringify({ error: "conversation_id and read_seq are required" }), {
        status: 400,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    // Verify conversation membership
    const { data: conv, error: convError } = await supabaseClient
      .from("chat_conversations")
      .select("id, customer_id, partner_id, last_message_seq")
      .eq("id", conversation_id)
      .single();

    if (convError || !conv) {
      return new Response(JSON.stringify({ error: "Conversation not found" }), {
        status: 404,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    if (conv.customer_id !== identity.profileId && conv.partner_id !== identity.profileId) {
      return new Response(JSON.stringify({ error: "Forbidden: Not participant of this conversation" }), {
        status: 403,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    const clampedSeq = Math.min(read_seq, Number(conv.last_message_seq || 0));

    // Fetch existing read state
    const { data: existingState } = await supabaseClient
      .from("chat_read_state")
      .select("last_read_seq, last_delivered_seq")
      .eq("conversation_id", conv.id)
      .eq("user_id", identity.profileId)
      .single();

    const newReadSeq = Math.max(Number(existingState?.last_read_seq || 0), clampedSeq);
    const newDeliveredSeq = Math.max(Number(existingState?.last_delivered_seq || 0), clampedSeq);

    await supabaseClient
      .from("chat_read_state")
      .upsert(
        {
          conversation_id: conv.id,
          user_id: identity.profileId,
          last_read_seq: newReadSeq,
          last_delivered_seq: newDeliveredSeq,
          updated_at: new Date().toISOString(),
        },
        { onConflict: "conversation_id, user_id" }
      );

    return new Response(
      JSON.stringify({
        success: true,
        last_read_seq: newReadSeq,
      }),
      {
        status: 200,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      }
    );
  } catch (err: any) {
    const status = err.message === "Unauthorized" || err.message?.includes("authorization") ? 401 : 500;
    return new Response(JSON.stringify({ error: err.message || "Internal server error" }), {
      status,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }
});
