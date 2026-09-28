import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.0";
import { getMasterKey, unwrapDek, decryptMessage, hexToBytes } from "../_shared/crypto.ts";
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
    if (!identity.isAdmin) {
      return jsonResponse({ error: "Forbidden: Admin access required" }, 403);
    }

    const url = new URL(req.url);
    const conversationId = url.searchParams.get("conversation_id");

    if (!conversationId) {
      return jsonResponse({ error: "conversation_id is required" }, 400);
    }

    // Write immutable audit record
    await supabaseClient.from("chat_admin_audit").insert({
      admin_id: identity.userId,
      conversation_id: conversationId,
      action: "OPEN",
    });

    // Fetch conversation
    const { data: conv, error: convError } = await supabaseClient
      .from("chat_conversations")
      .select(`
        id,
        booking_id,
        customer_id,
        partner_id,
        status,
        wrapped_dek,
        key_version,
        bookings (
          booking_code,
          service_name,
          customer_name
        )
      `)
      .eq("id", conversationId)
      .single();

    if (convError || !conv) {
      return jsonResponse({ error: "Conversation not found" }, 404);
    }

    // Fetch both parties' read states (Read only, NO writes)
    const { data: readStates } = await supabaseClient
      .from("chat_read_state")
      .select("user_id, last_read_seq, last_delivered_seq")
      .eq("conversation_id", conversationId);

    const custState = (readStates || []).find((r: any) => r.user_id === conv.customer_id) || { last_read_seq: 0, last_delivered_seq: 0 };
    const partnerState = (readStates || []).find((r: any) => r.user_id === conv.partner_id) || { last_read_seq: 0, last_delivered_seq: 0 };

    // Fetch all messages
    const { data: messages, error: msgError } = await supabaseClient
      .from("chat_messages")
      .select("id, seq, sender_id, sender_role, kind, ciphertext, nonce, created_at")
      .eq("conversation_id", conversationId)
      .order("seq", { ascending: true });

    if (msgError) throw msgError;

    const masterKey = await getMasterKey(Deno.env.get("CHAT_MASTER_KEY"));
    const dek = await unwrapDek(hexToBytes(conv.wrapped_dek), masterKey);

    const decryptedMessages = [];
    for (const msg of (messages || [])) {
      let text = "";
      try {
        text = await decryptMessage(
          hexToBytes(msg.ciphertext),
          hexToBytes(msg.nonce),
          dek,
          conv.id,
          msg.id,
          msg.sender_id,
          conv.key_version || 1
        );
      } catch {
        text = "This older message can't be displayed.";
      }

      decryptedMessages.push({
        id: msg.id,
        seq: msg.seq,
        sender_id: msg.sender_id,
        sender_role: msg.sender_role,
        kind: msg.kind,
        text,
        created_at: new Date(msg.created_at).toISOString(),
      });
    }

    return jsonResponse({
      conversation_id: conv.id,
      booking_id: conv.booking_id,
      booking_code: conv.bookings?.booking_code,
      service_name: conv.bookings?.service_name,
      customer_id: conv.customer_id,
      customer_name: conv.bookings?.customer_name,
      partner_id: conv.partner_id,
      status: conv.status,
      customer_read_seq: Number(custState.last_read_seq || 0),
      customer_delivered_seq: Number(custState.last_delivered_seq || 0),
      partner_read_seq: Number(partnerState.last_read_seq || 0),
      partner_delivered_seq: Number(partnerState.last_delivered_seq || 0),
      messages: decryptedMessages,
    });
  } catch (err: any) {
    return jsonError(err, "Failed to read admin conversation");
  }
});
