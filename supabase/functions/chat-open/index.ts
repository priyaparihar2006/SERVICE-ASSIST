import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.0";
import { getMasterKey, generateAndWrapDek, unwrapDek, encryptMessage, bytesToHex, hexToBytes } from "../_shared/crypto.ts";
import { resolveIdentity } from "../_shared/identity.ts";
import { recomputeBookingChatStatus } from "../_shared/chatStatus.ts";
import { jsonResponse, jsonError, defaultCorsHeaders } from "../_shared/http.ts";

async function injectEncryptedSystemMessage(
  supabaseClient: any,
  conv: any,
  text: string,
  kind: string = "BOOKING_UPDATE"
) {
  try {
    const masterKey = await getMasterKey(Deno.env.get("CHAT_MASTER_KEY"));
    const dek = await unwrapDek(hexToBytes(conv.wrapped_dek), masterKey);
    const msgId = crypto.randomUUID();

    const { ciphertext, nonce } = await encryptMessage(
      text,
      dek,
      conv.id,
      msgId,
      "SYSTEM",
      conv.key_version || 1
    );

    const { error: rpcError } = await supabaseClient.rpc("chat_append_message", {
      p_conv: conv.id,
      p_id: msgId,
      p_sender: "SYSTEM",
      p_role: "SYSTEM",
      p_kind: kind,
      p_ct: bytesToHex(ciphertext),
      p_nonce: bytesToHex(nonce),
    });

    if (rpcError) {
      console.error("[chat-open] system message append failed", rpcError.code, rpcError.message);
    }
  } catch (err) {
    console.error("Failed to inject system message:", err);
  }
}

serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: defaultCorsHeaders });

  try {
    const supabaseClient = createClient(
      Deno.env.get("SUPABASE_URL") ?? "",
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? ""
    );

    const identity = await resolveIdentity(req, supabaseClient);
    const profileId = identity.profileId;

    const body = await req.json().catch(() => ({}));
    const booking_id = body.booking_id;
    const booking_code = body.booking_code;

    if (!booking_id && !booking_code) {
      return jsonResponse({ error: "booking_id or booking_code is required" }, 400);
    }

    // 1. Fetch booking: check by booking_code first, then by id
    let booking = null;
    if (booking_code) {
      const { data } = await supabaseClient
        .from("bookings")
        .select("id, customer_id, customer_name, professional_id, status, accepted_at, service_name, booking_code, scheduled_date, scheduled_time")
        .eq("booking_code", booking_code)
        .maybeSingle();
      booking = data;
    }
    if (!booking && booking_id && typeof booking_id === "number" && booking_id > 0) {
      const { data } = await supabaseClient
        .from("bookings")
        .select("id, customer_id, customer_name, professional_id, status, accepted_at, service_name, booking_code, scheduled_date, scheduled_time")
        .eq("id", booking_id)
        .maybeSingle();
      booking = data;
    }

    if (!booking) {
      return jsonResponse(
        { error: "This booking hasn't synced to the server yet — pull to refresh and try again." },
        404
      );
    }

    // Validation: Partner must be assigned and have accepted the booking to open chat
    if (!booking.professional_id || booking.status === "PENDING" || booking.status === "CONFIRMED" || !booking.accepted_at) {
      return jsonResponse(
        { error: "Chat opens once a professional accepts your booking." },
        409
      );
    }

    // Authorization check
    const isCustomer = booking.customer_id === profileId;
    const isPartner = booking.professional_id === profileId;

    if (!isCustomer && !isPartner && !identity.isAdmin) {
      return jsonResponse({ error: "Forbidden: Not participant of this booking" }, 403);
    }

    // 2. Fetch or create conversation keyed strictly by booking_id
    let { data: conv } = await supabaseClient
      .from("chat_conversations")
      .select("id, status, booking_id, customer_id, partner_id, wrapped_dek, key_version, opened_by")
      .eq("booking_id", booking.id)
      .neq("status", "CLOSED")
      .maybeSingle();

    const isBookingActive = booking.status !== "COMPLETED" && booking.status !== "CANCELLED";
    const initialStatus = isBookingActive ? "ACTIVE" : "READ_ONLY";

    if (conv) {
      try {
        const masterKey = await getMasterKey(Deno.env.get("CHAT_MASTER_KEY"));
        await unwrapDek(hexToBytes(conv.wrapped_dek), masterKey);
      } catch (err: any) {
        if (err?.kind === "CONVERSATION_KEY") {
          console.warn(`[chat-open] Existing conv ${conv.id} cannot be decrypted. Archiving as KEY_LOST.`);
          await supabaseClient
            .from("chat_conversations")
            .update({
              status: "CLOSED",
              archived_reason: "KEY_LOST",
            })
            .eq("id", conv.id);
          conv = null;
        } else {
          // Re-throw MASTER_KEY or transient errors
          throw err;
        }
      }
    }

    if (conv) {
      // Recompute correct status based on booking
      await recomputeBookingChatStatus(supabaseClient, booking.id);

      // Re-fetch conversation to return current state
      const { data: updatedConv } = await supabaseClient
        .from("chat_conversations")
        .select("id, status, booking_id, customer_id, partner_id, wrapped_dek, key_version, opened_by")
        .eq("id", conv.id)
        .single();
      if (updatedConv) conv = updatedConv;
    } else {
      // Generate fresh DEK and create persistent conversation per booking
      const masterKey = await getMasterKey(Deno.env.get("CHAT_MASTER_KEY"));
      const { wrappedDek } = await generateAndWrapDek(masterKey);

      const insertPayload = {
        booking_id: booking.id,
        customer_id: booking.customer_id,
        partner_id: booking.professional_id,
        status: initialStatus,
        key_version: 1,
        wrapped_dek: bytesToHex(wrappedDek),
        opened_by: profileId,
      };

      const { data: newConv, error: insertError } = await supabaseClient
        .from("chat_conversations")
        .insert(insertPayload)
        .select("id, status, booking_id, customer_id, partner_id, wrapped_dek, key_version, opened_by")
        .maybeSingle();

      if (insertError) {
        // Race condition: another participant opened simultaneously (23505)
        if (insertError.code === "23505") {
          const { data: existingConv } = await supabaseClient
            .from("chat_conversations")
            .select("id, status, booking_id, customer_id, partner_id, wrapped_dek, key_version, opened_by")
            .eq("booking_id", booking.id)
            .neq("status", "CLOSED")
            .single();
          conv = existingConv;
        } else {
          throw insertError;
        }
      } else {
        conv = newConv;

        // Post welcome announcement
        const serviceName = booking.service_name || "Service";
        const bookingRef = booking.booking_code ? `#${booking.booking_code}` : `#${booking.id}`;
        let scheduleInfo = "";
        if (booking.scheduled_date) {
          scheduleInfo = ` for ${booking.scheduled_date}${booking.scheduled_time ? ` at ${booking.scheduled_time}` : ""}`;
        }
        const systemNotice = `New booking accepted: ${serviceName} (ID: ${bookingRef})${scheduleInfo}`;
        await injectEncryptedSystemMessage(supabaseClient, conv, systemNotice, "BOOKING_UPDATE");
      }
    }

    // Initialize read states for both participants (insert-if-missing only, never overwrite existing cursors)
    await supabaseClient
      .from("chat_read_state")
      .upsert(
        [
          {
            conversation_id: conv.id,
            user_id: conv.customer_id,
            last_read_seq: 0,
            last_delivered_seq: 0,
            updated_at: new Date().toISOString(),
          },
          {
            conversation_id: conv.id,
            user_id: conv.partner_id,
            last_read_seq: 0,
            last_delivered_seq: 0,
            updated_at: new Date().toISOString(),
          },
        ],
        { onConflict: "conversation_id, user_id", ignoreDuplicates: true }
      );

    let counterpartDisplayName = "Support";
    if (isCustomer) {
      if (booking.professional_id) {
        const { data: prof } = await supabaseClient
          .from("user_profiles")
          .select("name")
          .eq("id", booking.professional_id)
          .maybeSingle();
        counterpartDisplayName = prof?.name || "Professional Partner";
      } else {
        counterpartDisplayName = "Professional Partner";
      }
    } else {
      counterpartDisplayName = booking.customer_name || "Customer";
    }

    return jsonResponse({
      conversation_id: conv.id,
      status: conv.status,
      booking_id: conv.booking_id,
      booking_code: booking.booking_code || `#${booking.id}`,
      service_name: booking.service_name || "Service",
      counterpart_display_name: counterpartDisplayName,
    });
  } catch (err: any) {
    return jsonError(err, "Failed to open conversation");
  }
});
