import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.0";
import { resolveIdentity } from "../_shared/identity.ts";
import { recomputeBookingChatStatus } from "../_shared/chatStatus.ts";
import { jsonResponse, jsonError, defaultCorsHeaders } from "../_shared/http.ts";

serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: defaultCorsHeaders });

  try {
    const supabaseClient = createClient(
      Deno.env.get("SUPABASE_URL") ?? "",
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? ""
    );

    const identity = await resolveIdentity(req, supabaseClient);
    const body = await req.json().catch(() => ({}));

    const bookingId = body.booking_id ?? body.id;
    const bookingCode = body.booking_code;
    const newStatus = body.status;

    if (!newStatus) {
      return jsonResponse({ error: "status is required" }, 400);
    }

    if (!bookingId && !bookingCode) {
      return jsonResponse({ error: "booking_id or booking_code is required" }, 400);
    }

    // Lookup booking by id or booking_code
    let query = supabaseClient.from("bookings").select("*");
    if (bookingId && typeof bookingId === "number" && bookingId > 0) {
      query = query.eq("id", bookingId);
    } else if (bookingCode) {
      query = query.eq("booking_code", bookingCode);
    }

    const { data: booking, error: fetchError } = await query.maybeSingle();

    if (fetchError) {
      throw fetchError;
    }

    if (!booking) {
      return jsonResponse({ error: "Booking not found" }, 404);
    }

    // Role-based validation
    if (identity.role === "CUSTOMER") {
      if (newStatus.toUpperCase() !== "CANCELLED") {
        return jsonResponse({ error: "Customers can only cancel bookings" }, 403);
      }
      if (identity.isProd && booking.customer_id !== identity.profileId) {
        return jsonResponse({ error: "Cannot cancel a booking belonging to another customer" }, 403);
      }
    } else if (identity.role === "PARTNER") {
      if (identity.isProd && booking.professional_id && booking.professional_id !== identity.profileId) {
        return jsonResponse({ error: "Partner is not assigned to this booking" }, 403);
      }
      if (newStatus.toUpperCase() === "ASSIGNED") {
        return jsonResponse({ error: "Use Accept Job to accept this booking" }, 409);
      }
      const isAwaitingAcceptance = !booking.accepted_at || (booking.status ?? "").toUpperCase() === "CONFIRMED";
      if (isAwaitingAcceptance && ["ON_THE_WAY", "ARRIVED", "IN_PROGRESS", "COMPLETED"].includes(newStatus.toUpperCase())) {
        return jsonResponse({ error: "Accept the job first" }, 409);
      }
    }

    const updatePayload: Record<string, any> = {
      status: newStatus.toUpperCase(),
    };

    if (body.special_notes !== undefined) {
      updatePayload.special_notes = body.special_notes;
    }
    if (body.start_otp !== undefined) {
      updatePayload.start_otp = body.start_otp;
    }
    if (newStatus.toUpperCase() === "CANCELLED") {
      updatePayload.cancelled_by = body.cancelled_by ?? (identity.role === "PARTNER" ? "PARTNER" : "CUSTOMER");
      updatePayload.cancelled_at = new Date().toISOString();
      if (body.cancellation_reason !== undefined) {
        updatePayload.cancellation_reason = body.cancellation_reason;
      }
      if (body.cancellation_feedback !== undefined) {
        updatePayload.cancellation_feedback = body.cancellation_feedback;
      }
    }

    const { data: updatedBooking, error: updateError } = await supabaseClient
      .from("bookings")
      .update(updatePayload)
      .eq("id", booking.id)
      .select()
      .single();

    if (updateError) {
      throw updateError;
    }

    // Automatically recompute booking chat status (moves completed/cancelled chats to READ_ONLY)
    if (updatedBooking.id) {
      await recomputeBookingChatStatus(
        supabaseClient,
        updatedBooking.id
      );
    }

    return jsonResponse(updatedBooking);
  } catch (err: any) {
    return jsonError(err, "Failed to update booking status");
  }
});
