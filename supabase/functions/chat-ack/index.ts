import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.0";
import { resolveIdentity } from "../_shared/identity.ts";
import { jsonResponse, jsonError, defaultCorsHeaders } from "../_shared/http.ts";

serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: defaultCorsHeaders });

  try {
    const supabaseClient = createClient(
      Deno.env.get("SUPABASE_URL") ?? "",
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? ""
    );

    const identity = await resolveIdentity(req, supabaseClient);
    if (identity.isAdmin) {
      return jsonResponse({ error: "Forbidden: Admin cannot ack receipts" }, 403);
    }

    const { conversation_id, read_seq } = await req.json().catch(() => ({}));
    if (!conversation_id || typeof read_seq !== "number") {
      return jsonResponse({ error: "conversation_id and read_seq are required" }, 400);
    }

    // Verify conversation membership
    const { data: conv, error: convError } = await supabaseClient
      .from("chat_conversations")
      .select("id, customer_id, partner_id, last_message_seq")
      .eq("id", conversation_id)
      .single();

    if (convError || !conv) {
      return jsonResponse({ error: "Conversation not found" }, 404);
    }

    if (conv.customer_id !== identity.profileId && conv.partner_id !== identity.profileId) {
      return jsonResponse({ error: "Forbidden: Not participant of this conversation" }, 403);
    }

    const clampedSeq = Math.min(read_seq, Number(conv.last_message_seq || 0));

    // Call monotonic bump RPC
    const { error: bumpErr } = await supabaseClient.rpc("chat_bump_read_state", {
      p_conv: conv.id,
      p_user: identity.profileId,
      p_read: clampedSeq,
      p_delivered: clampedSeq,
    });

    if (bumpErr) {
      console.error("[chat-ack] chat_bump_read_state failed:", bumpErr.code, bumpErr.message);
    }

    return jsonResponse({
      success: true,
      last_read_seq: clampedSeq,
    });
  } catch (err: any) {
    return jsonError(err, "Failed to ack read receipt");
  }
});
