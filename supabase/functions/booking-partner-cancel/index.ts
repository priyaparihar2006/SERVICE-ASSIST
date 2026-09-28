import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.0";
import { resolveIdentity } from "../_shared/identity.ts";
import { recomputeBookingChatStatus } from "../_shared/chatStatus.ts";
import { jsonResponse, jsonError, defaultCorsHeaders } from "../_shared/http.ts";
import { scrubMessageContent } from "../_shared/scrubber.ts";

const PARTNER_CANCEL_REASONS: Record<string, string> = {
  EMERGENCY: "Personal / family emergency",
  VEHICLE_ISSUE: "Vehicle breakdown or travel problem",
  RUNNING_LATE: "Can't reach on time for this slot",
  LOCATION_ISSUE: "Location too far or unreachable",
  CUSTOMER_UNREACHABLE: "Customer not reachable / not responding",
  CUSTOMER_REQUEST: "Customer asked me to cancel",
  TOOLS_UNAVAILABLE: "Required tools or parts not available",
  SAFETY_CONCERN: "Safety concern at the location",
  OTHER: "Other reason",
};

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
    const reasonCode = (body.reason_code ?? "").toUpperCase();
    let reasonNote = (body.reason_note ?? body.feedback ?? "").trim();

    if (!bookingId) {
      return jsonResponse({ error: "booking_id is required" }, 400);
    }

    if (!reasonCode || !PARTNER_CANCEL_REASONS[reasonCode]) {
      return jsonResponse({ error: "A valid cancellation reason is required" }, 400);
    }

    if (reasonCode === "OTHER" && reasonNote.length < 10) {
      return jsonResponse({ error: "Please provide at least 10 characters explaining the reason" }, 400);
    }

    if (reasonNote.length > 300) {
      reasonNote = reasonNote.substring(0, 300);
    }

    // Scrub contact info if entered in note
    const scrubCheck = scrubMessageContent(reasonNote);
    if (scrubCheck.blocked) {
      return jsonResponse({ error: scrubCheck.userMessage || "Please avoid sharing phone numbers or contact details in cancellation notes" }, 400);
    }

    const reasonLabel = PARTNER_CANCEL_REASONS[reasonCode];

    // 1. Fetch booking
    const { data: booking, error: fetchError } = await supabaseClient
      .from("bookings")
      .select("*")
      .eq("id", bookingId)
      .maybeSingle();

    if (fetchError) {
      throw fetchError;
    }

    if (!booking) {
      return jsonResponse({ error: "Booking not found" }, 404);
    }

    // 2. Permission check
    if (identity.role === "PARTNER" && identity.isProd) {
      if (booking.professional_id && booking.professional_id !== identity.profileId) {
        return jsonResponse({ error: "You are not assigned to this booking" }, 403);
      }
    } else if (identity.role === "CUSTOMER") {
      return jsonResponse({ error: "Only service partners can perform partner cancellation" }, 403);
    }

    // 3. Idempotency: if already cancelled by partner
    if (booking.status === "CANCELLED" && booking.cancelled_by === "PARTNER") {
      return jsonResponse({
        success: true,
        booking_id: booking.id,
        status: "CANCELLED",
      });
    }

    // 4. Allowed status check: ASSIGNED, ON_THE_WAY, ARRIVED and accepted_at is not null
    const statusUpper = (booking.status ?? "").toUpperCase();
    if (statusUpper === "STARTED" || statusUpper === "AWAITING_PAYMENT" || statusUpper === "COMPLETED") {
      return jsonResponse({
        error: "This job can't be cancelled because work has already started",
      }, 409);
    }

    if (statusUpper !== "ASSIGNED" && statusUpper !== "ON_THE_WAY" && statusUpper !== "ARRIVED") {
      return jsonResponse({
        error: "This job cannot be cancelled in its current state",
      }, 409);
    }

    if (!booking.accepted_at) {
      return jsonResponse({
        error: "This job has not been accepted yet",
      }, 409);
    }

    const nowIso = new Date().toISOString();

    // 5. Update booking
    const { data: updatedBooking, error: updateError } = await supabaseClient
      .from("bookings")
      .update({
        status: "CANCELLED",
        cancelled_by: "PARTNER",
        cancelled_at: nowIso,
        cancellation_reason: reasonLabel,
        cancellation_feedback: reasonNote || null,
        partner_cancel_reason_code: reasonCode,
      })
      .eq("id", booking.id)
      .select()
      .single();

    if (updateError) {
      throw updateError;
    }

    // 6. Insert audit row into partner_job_cancellations
    try {
      await supabaseClient.from("partner_job_cancellations").insert({
        booking_id: booking.id,
        booking_code: booking.booking_code,
        partner_id: booking.professional_id || identity.profileId,
        customer_id: booking.customer_id,
        reason_code: reasonCode,
        reason_label: reasonLabel,
        reason_note: reasonNote || null,
        status_at_cancel: booking.status,
        accepted_at: booking.accepted_at,
        cancelled_at: nowIso,
      });
    } catch (auditErr) {
      console.error("Failed to insert partner_job_cancellations audit row:", auditErr);
    }

    // 7. Recompute chat status (moves chat to READ_ONLY)
    if (updatedBooking.id) {
      await recomputeBookingChatStatus(supabaseClient, updatedBooking.id);
    }

    return jsonResponse({
      success: true,
      booking_id: updatedBooking.id,
      status: "CANCELLED",
    });
  } catch (err: any) {
    return jsonError(err, "Failed to cancel job");
  }
});
