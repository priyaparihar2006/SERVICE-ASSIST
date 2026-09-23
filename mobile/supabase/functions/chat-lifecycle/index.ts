import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.0";
import { getMasterKey, generateAndWrapDek, encryptMessage, bytesToHex } from "../_shared/crypto.ts";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });

  try {
    const supabaseClient = createClient(
      Deno.env.get("SUPABASE_URL") ?? "",
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? ""
    );

    const { event, booking_id, new_status, new_partner_id } = await req.json();

    if (!booking_id) {
      return new Response(JSON.stringify({ error: "booking_id is required" }), {
        status: 400,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    if (new_status === "COMPLETED" || new_status === "CANCELLED") {
      // Transition conversation to READ_ONLY with closes_at 24h in the future
      const closesAt = new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString();
      await supabaseClient
        .from("chat_conversations")
        .update({
          status: "READ_ONLY",
          closes_at: closesAt,
        })
        .eq("booking_id", booking_id);

      return new Response(JSON.stringify({ success: true, status: "READ_ONLY", closes_at: closesAt }), {
        status: 200,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    if (event === "PARTNER_REASSIGNED" && new_partner_id) {
      // 1. Close existing conversation
      await supabaseClient
        .from("chat_conversations")
        .update({ status: "CLOSED", closes_at: new Date().toISOString() })
        .eq("booking_id", booking_id);

      // 2. Fetch booking
      const { data: booking } = await supabaseClient
        .from("bookings")
        .select("id, customer_id")
        .eq("id", booking_id)
        .single();

      if (booking) {
        // Open new conversation with new partner
        const masterKey = await getMasterKey(Deno.env.get("CHAT_MASTER_KEY"));
        const { dek, wrappedDek } = await generateAndWrapDek(masterKey);

        const { data: newConv } = await supabaseClient
          .from("chat_conversations")
          .insert({
            booking_id: booking.id,
            customer_id: booking.customer_id,
            partner_id: new_partner_id,
            status: "ACTIVE",
            key_version: 1,
            wrapped_dek: bytesToHex(wrappedDek),
            opened_by: booking.customer_id,
            last_message_seq: 1,
          })
          .select("id")
          .single();

        if (newConv) {
          // Post SYSTEM message
          const msgId = crypto.randomUUID();
          const { ciphertext, nonce } = await encryptMessage(
            "A new professional has been assigned to your service.",
            dek,
            newConv.id,
            msgId,
            "SYSTEM",
            1
          );

          await supabaseClient.from("chat_messages").insert({
            id: msgId,
            conversation_id: newConv.id,
            sender_id: "SYSTEM",
            sender_role: "SYSTEM",
            kind: "SYSTEM",
            ciphertext: bytesToHex(ciphertext),
            nonce: bytesToHex(nonce),
          });
        }
      }

      return new Response(JSON.stringify({ success: true, event: "REASSIGNED" }), {
        status: 200,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    return new Response(JSON.stringify({ success: true, message: "No action needed" }), {
      status: 200,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  } catch (err: any) {
    return new Response(JSON.stringify({ error: err.message || "Internal server error" }), {
      status: 500,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }
});
