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

    if (!bookingId) {
      return jsonResponse({ error: "booking_id is required" }, 400);
    }

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

    // 2. Permission check: Only assigned professional or admin
    if (identity.role === "PARTNER" && identity.isProd) {
      if (booking.professional_id && booking.professional_id !== identity.profileId) {
        return jsonResponse({ error: "You are not assigned to this booking" }, 403);
      }
    } else if (identity.role === "CUSTOMER") {
      return jsonResponse({ error: "Only service partners can accept bookings" }, 403);
    }

    // 3. Status availability check
    const statusUpper = (booking.status ?? "").toUpperCase();
    if (statusUpper === "CANCELLED" || statusUpper === "COMPLETED") {
      return jsonResponse({ error: "This job is no longer available" }, 409);
    }

    // 4. Idempotency: if already accepted, return existing accepted_at
    if (booking.accepted_at) {
      return jsonResponse({
        success: true,
        booking_id: booking.id,
        status: booking.status,
        accepted_at: booking.accepted_at,
      });
    }

    // 5. Update accepted_at and status to ASSIGNED
    const nowIso = new Date().toISOString();
    const { data: updatedBooking, error: updateError } = await supabaseClient
      .from("bookings")
      .update({ status: "ASSIGNED", accepted_at: nowIso })
      .eq("id", booking.id)
      .select()
      .single();

    if (updateError) {
      throw updateError;
    }

    // Recompute chat status to make it ACTIVE if needed
    if (updatedBooking.id) {
      await recomputeBookingChatStatus(supabaseClient, updatedBooking.id);
    }

    return jsonResponse({
      success: true,
      booking_id: updatedBooking.id,
      status: updatedBooking.status,
      accepted_at: updatedBooking.accepted_at,
    });
  } catch (err: any) {
    return jsonError(err, "Failed to accept booking");
  }
});
