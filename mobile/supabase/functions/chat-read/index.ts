import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.0";
import { getMasterKey, unwrapDek, decryptMessage, hexToBytes } from "../_shared/crypto.ts";
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
    const profileId = identity.profileId;

    const url = new URL(req.url);
    const conversationId = url.searchParams.get("conversation_id");
    const afterSeq = parseInt(url.searchParams.get("after_seq") ?? "0", 10);

    if (!conversationId) {
      return new Response(JSON.stringify({ error: "conversation_id is required" }), {
        status: 400,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    // Fetch conversation and verify membership
    const { data: conv, error: convError } = await supabaseClient
      .from("chat_conversations")
      .select("id, booking_id, customer_id, partner_id, status, last_message_seq, wrapped_dek, key_version")
      .eq("id", conversationId)
      .single();

    if (convError || !conv) {
      return new Response(JSON.stringify({ error: "Conversation not found" }), {
        status: 404,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    if (conv.customer_id !== profileId && conv.partner_id !== profileId) {
      return new Response(JSON.stringify({ error: "Forbidden: Not participant of this conversation" }), {
        status: 403,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    const isCustomer = conv.customer_id === profileId;
    const peerId = isCustomer ? conv.partner_id : conv.customer_id;

    // Fetch peer cursors
    const { data: peerReadState } = await supabaseClient
      .from("chat_read_state")
      .select("last_read_seq, last_delivered_seq")
      .eq("conversation_id", conversationId)
      .eq("user_id", peerId)
      .single();

    const peerReadSeq = Number(peerReadState?.last_read_seq || 0);
    const peerDeliveredSeq = Number(peerReadState?.last_delivered_seq || 0);

    // Fetch messages after seq
    const { data: messages, error: msgError } = await supabaseClient
      .from("chat_messages")
      .select("id, seq, sender_id, sender_role, kind, ciphertext, nonce, created_at")
      .eq("conversation_id", conversationId)
      .gt("seq", afterSeq)
      .order("seq", { ascending: true })
      .limit(100);

    if (msgError) throw msgError;

    const masterKey = await getMasterKey(Deno.env.get("CHAT_MASTER_KEY"));
    const dek = await unwrapDek(hexToBytes(conv.wrapped_dek), masterKey);

    const decryptedMessages = [];
    let highestReturnedSeq = afterSeq;

    for (const msg of (messages || [])) {
      if (msg.seq > highestReturnedSeq) highestReturnedSeq = msg.seq;
      let text = "";
      try {
        text = await decryptMessage(
          hexToBytes(msg.ciphertext),
          hexToBytes(msg.nonce),
          dek,
          conv.id,
          msg.id,
          msg.sender_id,
          conv.key_version
        );
      } catch {
        text = "[Decryption Error]";
      }

      decryptedMessages.push({
        id: msg.id,
        seq: msg.seq,
        sender_id: msg.sender_id,
        sender_role: msg.sender_role,
        kind: msg.kind,
        text,
        created_at: new Date(msg.created_at).toISOString(),
        is_mine: msg.sender_id === profileId,
      });
    }

    // Mark delivered up to highest returned seq (only if messages were received)
    if (highestReturnedSeq > afterSeq && !identity.isAdmin) {
      const { data: myReadState } = await supabaseClient
        .from("chat_read_state")
        .select("last_delivered_seq")
        .eq("conversation_id", conversationId)
        .eq("user_id", profileId)
        .single();

      const newDelivered = Math.max(Number(myReadState?.last_delivered_seq || 0), highestReturnedSeq);
      await supabaseClient
        .from("chat_read_state")
        .upsert(
          {
            conversation_id: conv.id,
            user_id: profileId,
            last_delivered_seq: newDelivered,
            updated_at: new Date().toISOString(),
          },
          { onConflict: "conversation_id, user_id" }
        );
    }

    return new Response(
      JSON.stringify({
        conversation_id: conv.id,
        status: conv.status,
        messages: decryptedMessages,
        last_seq: Number(conv.last_message_seq || highestReturnedSeq),
        peer_read_seq: peerReadSeq,
        peer_delivered_seq: peerDeliveredSeq,
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
