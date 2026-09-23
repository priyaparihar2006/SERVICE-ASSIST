import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.0";
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
    const body = await req.json().catch(() => ({}));
    const { booking_id: bookingId, booking_code: bookingCode, utr } = body;

    if (!bookingId && !bookingCode) {
      return new Response(JSON.stringify({ error: "booking_id or booking_code is required" }), {
        status: 400,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    // 1. Strict UTR Validation (12 digits)
    const cleanUtr = typeof utr === "string" ? utr.trim() : "";
    if (!/^\d{12}$/.test(cleanUtr)) {
      return new Response(
        JSON.stringify({ error: "Invalid UTR. Please enter a valid 12-digit UPI reference number (UTR)." }),
        {
          status: 400,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        }
      );
    }

    // 2. Fetch booking by id or booking_code
    let booking = null;
    if (bookingId && typeof bookingId === "number" && bookingId > 0) {
      const { data } = await supabaseClient
        .from("bookings")
        .select("id, booking_code, total_amount, status, professional_id, customer_id")
        .eq("id", bookingId)
        .maybeSingle();
      booking = data;
    }
    if (!booking && bookingCode) {
      const { data } = await supabaseClient
        .from("bookings")
        .select("id, booking_code, total_amount, status, professional_id, customer_id")
        .eq("booking_code", bookingCode)
        .maybeSingle();
      booking = data;
    }
    if (!booking && bookingId) {
      const { data } = await supabaseClient
        .from("bookings")
        .select("id, booking_code, total_amount, status, professional_id, customer_id")
        .eq("booking_code", String(bookingId))
        .maybeSingle();
      booking = data;
    }

    if (!booking) {
      return new Response(JSON.stringify({ error: "Booking not found" }), {
        status: 404,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    // Verify caller is assigned partner or admin
    const isPartner = booking.professional_id === identity.profileId || (!identity.isProd && (identity.role === "PARTNER" || identity.role === "ADMIN"));
    if (!isPartner && !identity.isAdmin) {
      return new Response(JSON.stringify({ error: "Only the assigned partner can confirm UPI payment" }), {
        status: 403,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    const allowedStatuses = ["ARRIVED", "STARTED", "AWAITING_PAYMENT"];
    if (!allowedStatuses.includes(booking.status)) {
      return new Response(
        JSON.stringify({ error: `Booking is not ready for payment (current status: ${booking.status})` }),
        {
          status: 400,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        }
      );
    }

    const nowIso = new Date().toISOString();
    const nowMillis = Date.now();

    // 3. Upsert payments row first with status='PAID'
    const { data: payment, error: paymentError } = await supabaseClient
      .from("payments")
      .upsert(
        {
          booking_id: booking.id,
          amount: booking.total_amount,
          currency: "INR",
          method: "UPI",
          status: "PAID",
          upi_reference: cleanUtr,
          confirmed_by: identity.profileId,
          confirmed_role: (identity.role || "PARTNER").toUpperCase(),
          confirmation_note: "UPI verified with customer reference",
          paid_at: nowIso,
          updated_at: nowIso,
        },
        { onConflict: "booking_id" }
      )
      .select()
      .single();

    if (paymentError || !payment) {
      throw new Error(`Failed to record payment: ${paymentError?.message}`);
    }

    // 4. Update bookings to COMPLETED, is_paid=true, payment_method='UPI', payment_reference=utr
    const { data: updatedBooking, error: bookingUpdateError } = await supabaseClient
      .from("bookings")
      .update({
        status: "COMPLETED",
        is_paid: true,
        payment_method: "UPI",
        payment_reference: cleanUtr,
        paid_at: nowMillis,
      })
      .eq("id", booking.id)
      .select()
      .single();

    if (bookingUpdateError) {
      throw new Error(`Failed to update booking status: ${bookingUpdateError.message}`);
    }

    // 5. Record audit log (mask UTR in detail)
    const maskedUtr = cleanUtr.slice(-4).padStart(12, "*");
    await supabaseClient.from("payment_audit_log").insert({
      payment_id: payment.id,
      booking_id: booking.id,
      actor_id: identity.profileId,
      actor_role: (identity.role || "PARTNER").toUpperCase(),
      action: "UPI_CONFIRMED",
      detail: `UPI confirmed with UTR ${maskedUtr}`,
    });

    // 6. Close chat conversation (24h)
    const closesAt = new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString();
    await supabaseClient
      .from("chat_conversations")
      .update({ status: "READ_ONLY", closes_at: closesAt })
      .eq("booking_id", booking.id);

    return new Response(
      JSON.stringify({
        success: true,
        booking_id: booking.id,
        status: "COMPLETED",
        is_paid: true,
        payment_method: "UPI",
        payment_reference: cleanUtr,
        paid_at: nowMillis,
        payment_id: payment.id,
      }),
      {
        status: 200,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      }
    );
  } catch (err: any) {
    return new Response(JSON.stringify({ error: err.message || "Internal server error" }), {
      status: 500,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }
});
