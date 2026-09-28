import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.0";
import { getMasterKey, unwrapDek, decryptMessage, hexToBytes } from "../_shared/crypto.ts";
import { resolveIdentity } from "../_shared/identity.ts";
import { jsonResponse, jsonError, defaultCorsHeaders } from "../_shared/http.ts";

serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: defaultCorsHeaders });

  let stage = "identity";
  try {
    const supabaseClient = createClient(
      Deno.env.get("SUPABASE_URL") ?? "",
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? ""
    );

    const identity = await resolveIdentity(req, supabaseClient);
    const profileId = identity.profileId;

    stage = "rpc";
    // Call RPC chat_list_summaries
    const { data: rows, error: rpcError } = await supabaseClient.rpc("chat_list_summaries", {
      p_profile_id: profileId,
    });

    if (rpcError) {
      console.error("[chat-list] chat_list_summaries RPC error:", rpcError);
      throw rpcError;
    }

    const conversations = rows || [];
    if (conversations.length === 0) {
      return jsonResponse({ conversations: [] });
    }

    stage = "master_key";
    const masterKey = await getMasterKey(Deno.env.get("CHAT_MASTER_KEY"));

    stage = "decrypt";
    // Decrypt last messages concurrently
    const results = await Promise.all(
      conversations.map(async (conv: any) => {
        const isCustomer = conv.customer_id === profileId;
        let counterpartDisplayName = "";

        if (isCustomer) {
          const partnerFullName = conv.partner_name || "Professional";
          const partnerFirstName = partnerFullName.split(" ")[0] || "Professional";
          counterpartDisplayName = `${partnerFirstName} · Your Professional`;
        } else {
          const custFullName = conv.customer_name || "Client";
          const custFirstName = custFullName.split(" ")[0] || "Client";
          counterpartDisplayName = `${custFirstName} · Client`;
        }

        let lastMessagePreview = "No messages yet";
        let lastMessageFromMe = false;

        if (conv.last_msg_ct && conv.last_msg_nonce) {
          lastMessageFromMe = conv.last_msg_sender === profileId;
          try {
            const dek = await unwrapDek(hexToBytes(conv.wrapped_dek), masterKey);
            lastMessagePreview = await decryptMessage(
              hexToBytes(conv.last_msg_ct),
              hexToBytes(conv.last_msg_nonce),
              dek,
              conv.conversation_id,
              String(conv.last_msg_id || ""),
              conv.last_msg_sender,
              conv.key_version || 1
            );
          } catch {
            lastMessagePreview = "This older message can't be displayed.";
          }
        }

        let formattedLastMessageAt: string | null = null;
        if (conv.last_message_at) {
          const t = Date.parse(conv.last_message_at);
          if (Number.isFinite(t)) formattedLastMessageAt = new Date(t).toISOString();
        } else if (conv.opened_at) {
          const t = Date.parse(conv.opened_at);
          if (Number.isFinite(t)) formattedLastMessageAt = new Date(t).toISOString();
        }

        return {
          conversation_id: conv.conversation_id,
          booking_id: Number(conv.booking_id || 0),
          booking_code: conv.booking_code || "",
          service_name: conv.service_name || "Doorstep Service",
          counterpart_display_name: counterpartDisplayName,
          last_message_preview: lastMessagePreview,
          last_message_at: formattedLastMessageAt,
          last_message_from_me: lastMessageFromMe,
          unread_count: Number(conv.unread_count || 0),
          status: conv.status || "ACTIVE",
          peer_read_seq: Number(conv.peer_last_read || 0),
          peer_delivered_seq: Number(conv.peer_last_delivered || 0),
          latest_seq: Number(conv.latest_seq || 0),
        };
      })
    );

    stage = "respond";
    return jsonResponse({ conversations: results });
  } catch (err: any) {
    console.error(`[chat-list] stage=${stage} ${err?.name || "Error"} ${err?.code || ""} ${err?.message}`);
    return jsonError(err, "Failed to list conversations");
  }
});
