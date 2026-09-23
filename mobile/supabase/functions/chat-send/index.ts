import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.0";
import { getMasterKey, unwrapDek, encryptMessage, hexToBytes, bytesToHex } from "../_shared/crypto.ts";
import { scrubMessageContent } from "../_shared/scrubber.ts";
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

    const body = await req.json();
    const {
      conversation_id,
      client_message_id,
      text,
      kind = "TEXT",
    } = body;

    if (!conversation_id || !client_message_id || !text) {
      return new Response(JSON.stringify({ error: "Missing required fields" }), {
        status: 400,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    if (text.length > 1000) {
      return new Response(JSON.stringify({ error: "Message exceeds maximum length of 1000 characters" }), {
        status: 400,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    // Fetch conversation
    const { data: conv, error: convError } = await supabaseClient
      .from("chat_conversations")
      .select("id, booking_id, customer_id, partner_id, status, wrapped_dek, key_version")
      .eq("id", conversation_id)
      .single();

    if (convError || !conv) {
      return new Response(JSON.stringify({ error: "Conversation not found" }), {
        status: 404,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    if (conv.status !== "ACTIVE") {
      return new Response(JSON.stringify({ error: "Conversation is closed or read-only" }), {
        status: 403,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    const isCustomer = conv.customer_id === profileId;
    const isPartner = conv.partner_id === profileId;

    if (!isCustomer && !isPartner) {
      return new Response(JSON.stringify({ error: "Forbidden: Not participant of this conversation" }), {
        status: 403,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    const senderRole = isCustomer ? "CUSTOMER" : "PARTNER";

    // 1. Contact Scrubber
    if (kind === "TEXT") {
      const scrubResult = scrubMessageContent(text);
      if (scrubResult.blocked) {
        await supabaseClient.from("chat_flags").insert({
          conversation_id: conv.id,
          sender_id: profileId,
          reason: scrubResult.reason || "PHONE",
        });

        return new Response(
          JSON.stringify({
            error: scrubResult.userMessage || "Contact sharing is restricted. Keep chatting in the app.",
            code: "CONTACT_SHARING_RESTRICTED",
          }),
          {
            status: 400,
            headers: { ...corsHeaders, "Content-Type": "application/json" },
          }
        );
      }
    }

    // 2. Encrypt message
    const masterKey = await getMasterKey(Deno.env.get("CHAT_MASTER_KEY"));
    const dek = await unwrapDek(hexToBytes(conv.wrapped_dek), masterKey);

    const { ciphertext, nonce } = await encryptMessage(
      text,
      dek,
      conv.id,
      client_message_id,
      profileId,
      conv.key_version
    );

    // 3. Insert message
    const { data: insertedMsg, error: insertError } = await supabaseClient
      .from("chat_messages")
      .upsert(
        {
          id: client_message_id,
          conversation_id: conv.id,
          sender_id: profileId,
          sender_role: senderRole,
          kind,
          ciphertext: bytesToHex(ciphertext),
          nonce: bytesToHex(nonce),
        },
        { onConflict: "id" }
      )
      .select("id, seq, created_at")
      .single();

    if (insertError) throw insertError;

    const nowIso = new Date().toISOString();

    // 4. Update conversation last_message_at and last_message_seq
    await supabaseClient
      .from("chat_conversations")
      .update({
        last_message_at: nowIso,
        last_message_seq: insertedMsg.seq,
      })
      .eq("id", conv.id);

    // 5. Update sender's own read and delivery state to new seq
    await supabaseClient
      .from("chat_read_state")
      .upsert(
        {
          conversation_id: conv.id,
          user_id: profileId,
          last_read_seq: insertedMsg.seq,
          last_delivered_seq: insertedMsg.seq,
          updated_at: nowIso,
        },
        { onConflict: "conversation_id, user_id" }
      );

    return new Response(
      JSON.stringify({
        success: true,
        message_id: insertedMsg.id,
        seq: insertedMsg.seq,
        created_at: new Date(insertedMsg.created_at).toISOString(),
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
