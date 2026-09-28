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

    const url = new URL(req.url);
    let bookingId = url.searchParams.get("booking_id");
    let bookingCode = url.searchParams.get("booking_code");
    if ((!bookingId || !bookingCode) && req.method === "POST") {
      const body = await req.json().catch(() => ({}));
      bookingId = bookingId || body.booking_id;
      bookingCode = bookingCode || body.booking_code;
    }

    if (!bookingId && !bookingCode) {
      return new Response(JSON.stringify({ error: "booking_id or booking_code is required" }), {
        status: 400,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    // 1. Fetch booking to check authorization
    let booking = null;
    const numId = Number(bookingId);
    if (!isNaN(numId) && numId > 0) {
      const { data } = await supabaseClient
        .from("bookings")
        .select("id, customer_id, professional_id, status, total_amount, payment_method, is_paid")
        .eq("id", numId)
        .maybeSingle();
      booking = data;
    }
    if (!booking && bookingCode) {
      const { data } = await supabaseClient
        .from("bookings")
        .select("id, customer_id, professional_id, status, total_amount, payment_method, is_paid")
        .eq("booking_code", bookingCode)
        .maybeSingle();
      booking = data;
    }
    if (!booking && bookingId) {
      const { data } = await supabaseClient
        .from("bookings")
        .select("id, customer_id, professional_id, status, total_amount, payment_method, is_paid")
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

    const isCustomer = booking.customer_id === identity.profileId;
    const isPartner = booking.professional_id === identity.profileId || (!identity.isProd && (identity.role === "PARTNER" || identity.role === "ADMIN"));
    if (!isCustomer && !isPartner && !identity.isAdmin) {
      return new Response(JSON.stringify({ error: "Unauthorized" }), {
        status: 403,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    // 2. Fetch payment
    const { data: payment } = await supabaseClient
      .from("payments")
      .select("id, booking_id, amount, currency, method, status, upi_vpa, upi_reference, qr_payload, confirmed_by, confirmed_role, paid_at, created_at")
      .eq("booking_id", booking.id)
      .maybeSingle();

    if (!payment) {
      return new Response(
        JSON.stringify({
          found: false,
          booking_status: booking.status,
          is_paid: booking.is_paid,
          payment_method: booking.payment_method,
        }),
        {
          status: 200,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        }
      );
    }

    return new Response(
      JSON.stringify({
        found: true,
        booking_status: booking.status,
        is_paid: booking.is_paid,
        payment: {
          id: payment.id,
          booking_id: payment.booking_id,
          amount: payment.amount,
          currency: payment.currency,
          method: payment.method,
          status: payment.status,
          upi_vpa: payment.upi_vpa,
          upi_reference: payment.upi_reference,
          qr_payload: payment.qr_payload,
          confirmed_by: payment.confirmed_by,
          confirmed_role: payment.confirmed_role,
          paid_at: payment.paid_at,
          created_at: payment.created_at,
        },
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
