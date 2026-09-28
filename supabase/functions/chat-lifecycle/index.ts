import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.0";
import { getMasterKey, generateAndWrapDek, encryptMessage, bytesToHex } from "../_shared/crypto.ts";
import { recomputeBookingChatStatus } from "../_shared/chatStatus.ts";
import { jsonResponse, jsonError, defaultCorsHeaders } from "../_shared/http.ts";

serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: defaultCorsHeaders });

  try {
    const supabaseClient = createClient(
      Deno.env.get("SUPABASE_URL") ?? "",
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? ""
    );

    const { event, booking_id, new_status, new_partner_id } = await req.json().catch(() => ({}));

    if (!booking_id) {
      return jsonResponse({ error: "booking_id is required" }, 400);
    }

    // Fetch booking
    const { data: booking, error: bErr } = await supabaseClient
      .from("bookings")
      .select("id, customer_id, professional_id, status, service_name, booking_code")
      .eq("id", booking_id)
      .single();

    if (bErr || !booking) {
      return jsonResponse({ error: "Booking not found" }, 404);
    }

    if (new_status === "COMPLETED" || new_status === "CANCELLED") {
      const updatedStatus = await recomputeBookingChatStatus(
        supabaseClient,
        booking.id
      );

      return jsonResponse({ success: true, status: updatedStatus });
    }

    if (event === "PARTNER_REASSIGNED" && new_partner_id) {
      // 1. Close old partner conversation for this booking
      const { error: closeErr } = await supabaseClient
        .from("chat_conversations")
        .update({
          status: "CLOSED",
          archived_reason: "REASSIGNED",
          closes_at: new Date().toISOString(),
        })
        .eq("booking_id", booking.id)
        .neq("status", "CLOSED");

      if (closeErr) {
        console.error("[chat-lifecycle] close old conv error:", closeErr.code, closeErr.message);
      }

      // 2. Open fresh conversation for new partner
      const masterKey = await getMasterKey(Deno.env.get("CHAT_MASTER_KEY"));
      const { wrappedDek } = await generateAndWrapDek(masterKey);

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
        })
        .select("id, status, wrapped_dek, key_version")
        .maybeSingle();

      return jsonResponse({ success: true, event: "REASSIGNED", conversation_id: newConv?.id });
    }

    return jsonResponse({ success: true, message: "No action needed" });
  } catch (err: any) {
    return jsonError(err, "Failed to update chat lifecycle");
  }
});
