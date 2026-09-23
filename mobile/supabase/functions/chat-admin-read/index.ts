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
    if (!identity.isAdmin) {
      return new Response(JSON.stringify({ error: "Forbidden: Admin access required" }), {
        status: 403,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    const url = new URL(req.url);
    const conversationId = url.searchParams.get("conversation_id");

    if (!conversationId) {
      return new Response(JSON.stringify({ error: "conversation_id is required" }), {
        status: 400,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
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
      return new Response(JSON.stringify({ error: "Conversation not found" }), {
        status: 404,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
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
        is_mine: false,
      });
    }

    return new Response(
      JSON.stringify({
        conversation_id: conv.id,
        booking_id: conv.booking_id,
        booking_code: conv.bookings?.booking_code,
        service_name: conv.bookings?.service_name,
        customer_name: conv.bookings?.customer_name,
        status: conv.status,
        is_admin_view: true,
        audit_notice: "Read-only · Admin audit view · This access is logged",
        messages: decryptedMessages,
        customer_read_seq: Number(custState.last_read_seq || 0),
        customer_delivered_seq: Number(custState.last_delivered_seq || 0),
        partner_read_seq: Number(partnerState.last_read_seq || 0),
        partner_delivered_seq: Number(partnerState.last_delivered_seq || 0),
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
