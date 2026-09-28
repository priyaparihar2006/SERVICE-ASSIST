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

    const url = new URL(req.url);
    const conversationId = url.searchParams.get("conversation_id");
    const afterSeq = parseInt(url.searchParams.get("after_seq") ?? "0", 10);
    const beforeSeqParam = url.searchParams.get("before_seq");
    const beforeSeq = beforeSeqParam ? parseInt(beforeSeqParam, 10) : null;

    if (!conversationId) {
      return jsonResponse({ error: "conversation_id is required" }, 400);
    }

    stage = "conv";
    // Fetch conversation and verify membership
    const { data: conv, error: convError } = await supabaseClient
      .from("chat_conversations")
      .select("id, booking_id, customer_id, partner_id, status, last_message_seq, wrapped_dek, key_version")
      .eq("id", conversationId)
      .single();

    if (convError || !conv) {
      return jsonResponse({ error: "Conversation not found" }, 404);
    }

    if (conv.customer_id !== profileId && conv.partner_id !== profileId && !identity.isAdmin) {
      return jsonResponse({ error: "Forbidden: Not participant of this conversation" }, 403);
    }

    const isCustomer = conv.customer_id === profileId;
    const peerId = isCustomer ? conv.partner_id : conv.customer_id;

    stage = "peer_state";
    // Fetch peer cursors
    const { data: peerReadState } = await supabaseClient
      .from("chat_read_state")
      .select("last_read_seq, last_delivered_seq")
      .eq("conversation_id", conversationId)
      .eq("user_id", peerId)
      .maybeSingle();

    const peerReadSeq = Number(peerReadState?.last_read_seq || 0);
    const peerDeliveredSeq = Number(peerReadState?.last_delivered_seq || 0);

    stage = "messages";
    // Fetch messages:
    let query = supabaseClient
      .from("chat_messages")
      .select("id, seq, sender_id, sender_role, kind, ciphertext, nonce, created_at")
      .eq("conversation_id", conversationId);

    let isLatestFirst = false;
    if (beforeSeq !== null && beforeSeq > 0) {
      query = query.lt("seq", beforeSeq).order("seq", { ascending: false }).limit(50);
      isLatestFirst = true;
    } else if (afterSeq === 0) {
      query = query.order("seq", { ascending: false }).limit(50);
      isLatestFirst = true;
    } else {
      query = query.gt("seq", afterSeq).order("seq", { ascending: true }).limit(100);
    }

    const { data: rawMessages, error: msgError } = await query;
    if (msgError) throw msgError;

    let messages = rawMessages || [];
    if (isLatestFirst) {
      messages = messages.reverse();
    }

    stage = "master_key";
    const masterKey = await getMasterKey(Deno.env.get("CHAT_MASTER_KEY"));

    stage = "unwrap";
    const dek = await unwrapDek(hexToBytes(conv.wrapped_dek), masterKey);

    stage = "decrypt";
    const decryptedMessages = [];
    let highestReturnedSeq = afterSeq;

    for (const msg of messages) {
      const msgSeq = Number(msg.seq || 0);
      if (msgSeq > highestReturnedSeq) highestReturnedSeq = msgSeq;
      let text = "";
      let msgKind = msg.kind || "TEXT";
      if (msg.ciphertext && msg.nonce) {
        try {
          text = await decryptMessage(
            hexToBytes(msg.ciphertext),
            hexToBytes(msg.nonce),
            dek,
            conv.id,
            String(msg.id),
            msg.sender_id,
            conv.key_version || 1
          );
        } catch {
          text = "This older message can't be displayed.";
          msgKind = "SYSTEM";
        }
      } else {
        text = "This older message can't be displayed.";
        msgKind = "SYSTEM";
      }

      const t = Date.parse(msg.created_at);
      const safeCreatedAt = Number.isFinite(t) ? new Date(t).toISOString() : new Date().toISOString();

      decryptedMessages.push({
        id: String(msg.id),
        seq: msgSeq,
        sender_id: msg.sender_id,
        sender_role: msg.sender_role,
        kind: msgKind,
        text,
        created_at: safeCreatedAt,
        is_mine: msg.sender_id === profileId,
      });
    }

    stage = "bump";
    // Mark delivered up to highest returned seq (only if messages were received)
    if (highestReturnedSeq > afterSeq && !identity.isAdmin) {
      const { error: bumpErr } = await supabaseClient.rpc("chat_bump_read_state", {
        p_conv: conv.id,
        p_user: profileId,
        p_read: 0,
        p_delivered: highestReturnedSeq,
      });
      if (bumpErr) {
        console.error("[chat-read] chat_bump_read_state failed:", bumpErr.code, bumpErr.message);
      }
    }

    stage = "respond";
    const batchHighestSeq = decryptedMessages.length > 0 
      ? Math.max(...decryptedMessages.map(m => Number(m.seq) || 0)) 
      : afterSeq;

    const safeLastSeq = Number.isFinite(Number(batchHighestSeq)) ? Number(batchHighestSeq) : Number(afterSeq || 0);
    const safeLatestSeq = Number.isFinite(Number(conv.last_message_seq)) 
      ? Number(conv.last_message_seq) 
      : safeLastSeq;

    return jsonResponse({
      conversation_id: conv.id,
      status: conv.status || "ACTIVE",
      messages: decryptedMessages,
      last_seq: safeLastSeq,
      latest_seq: safeLatestSeq,
      peer_read_seq: Number(peerReadSeq || 0),
      peer_delivered_seq: Number(peerDeliveredSeq || 0),
    });
  } catch (err: any) {
    console.error(`[chat-read] stage=${stage} ${err?.name || "Error"} ${err?.code || ""} ${err?.message}`);
    if (err?.name === "ChatCryptoError" || err?.kind) {
      return jsonError(err, "Failed to read messages");
    }
    return jsonResponse({ error: "Failed to read messages", code: "READ_FAILED", stage }, 500);
  }
});
