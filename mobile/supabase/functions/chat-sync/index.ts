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
      return new Response(JSON.stringify({ error: "Forbidden: Admin cannot call chat-sync" }), {
        status: 403,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    // Call RPC chat_sync
    const { data: syncRows, error: rpcError } = await supabaseClient.rpc("chat_sync", {
      p_profile_id: identity.profileId,
    });

    if (rpcError) throw rpcError;

    // Update last_delivered_seq = latest_seq for each conversation
    const conversations = syncRows || [];
    for (const conv of conversations) {
      if (conv.latest_seq && conv.latest_seq > 0) {
        await supabaseClient
          .from("chat_read_state")
          .upsert(
            {
              conversation_id: conv.conversation_id,
              user_id: identity.profileId,
              last_delivered_seq: conv.latest_seq,
              updated_at: new Date().toISOString(),
            },
            { onConflict: "conversation_id, user_id" }
          );
      }
    }

    const totalUnread = conversations.reduce((sum: number, c: any) => sum + Number(c.unread_count || 0), 0);

    return new Response(
      JSON.stringify({
        conversations,
        total_unread: totalUnread,
        server_time: new Date().toISOString(),
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
