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
    const bookingId = body.booking_id;
    const bookingCode = body.booking_code;

    if (!bookingId && !bookingCode) {
      return new Response(JSON.stringify({ error: "booking_id or booking_code is required" }), {
        status: 400,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    // 1. Fetch booking by id or booking_code
    let booking = null;
    if (bookingId && typeof bookingId === "number" && bookingId > 0) {
      const { data } = await supabaseClient
        .from("bookings")
        .select("id, booking_code, service_name, total_amount, status, customer_id, professional_id")
        .eq("id", bookingId)
        .maybeSingle();
      booking = data;
    }
    if (!booking && bookingCode) {
      const { data } = await supabaseClient
        .from("bookings")
        .select("id, booking_code, service_name, total_amount, status, customer_id, professional_id")
        .eq("booking_code", bookingCode)
        .maybeSingle();
      booking = data;
    }
    if (!booking && bookingId) {
      const { data } = await supabaseClient
        .from("bookings")
        .select("id, booking_code, service_name, total_amount, status, customer_id, professional_id")
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

    // Verify caller is customer, partner, or admin
    const isCustomer = booking.customer_id === identity.profileId;
    const isPartner = booking.professional_id === identity.profileId || (!identity.isProd && (identity.role === "PARTNER" || identity.role === "ADMIN"));
    if (!isCustomer && !isPartner && !identity.isAdmin) {
      return new Response(JSON.stringify({ error: "Unauthorized access to this booking" }), {
        status: 403,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    if (booking.status === "COMPLETED") {
      return new Response(JSON.stringify({ error: "Booking is already completed" }), {
        status: 400,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    const allowedStatuses = ["ARRIVED", "STARTED", "AWAITING_PAYMENT"];
    if (!allowedStatuses.includes(booking.status)) {
      return new Response(
        JSON.stringify({ error: `Cannot initiate payment for booking with status ${booking.status}. Partner must arrive before payment.` }),
        {
          status: 400,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        }
      );
    }

    // 2. Fetch UPI configuration from app_config
    const { data: configs } = await supabaseClient
      .from("app_config")
      .select("key, value")
      .in("key", ["upi_payee_vpa", "upi_payee_name"]);

    let upiVpa = "9105830551@upi";
    let upiName = "Servora";
    configs?.forEach((c: { key: string; value: string }) => {
      if (c.key === "upi_payee_vpa") upiVpa = c.value;
      if (c.key === "upi_payee_name") upiName = c.value;
    });

    // 3. Build UPI deep link string
    const bookingCode = booking.booking_code || `SRV-${booking.id}`;
    const amountStr = `${booking.total_amount}.00`;
    const note = `Servora booking ${bookingCode}`;
    const qrPayload = `upi://pay?pa=${encodeURIComponent(upiVpa)}&pn=${encodeURIComponent(upiName)}&am=${amountStr}&cu=INR&tn=${encodeURIComponent(note)}&tr=${encodeURIComponent(bookingCode)}`;

    // 4. Check existing payment
    const { data: existingPayment } = await supabaseClient
      .from("payments")
      .select("*")
      .eq("booking_id", booking.id)
      .maybeSingle();

    let paymentRecord = existingPayment;

    if (!existingPayment) {
      const { data: newPayment, error: insertError } = await supabaseClient
        .from("payments")
        .insert({
          booking_id: booking.id,
          amount: booking.total_amount,
          currency: "INR",
          method: "UPI",
          status: "PENDING",
          upi_vpa: upiVpa,
          qr_payload: qrPayload,
        })
        .select()
        .single();

      if (insertError) {
        throw new Error(insertError.message);
      }
      paymentRecord = newPayment;
    } else if (existingPayment.status === "PENDING") {
      const { data: updatedPayment, error: updateError } = await supabaseClient
        .from("payments")
        .update({
          upi_vpa: upiVpa,
          qr_payload: qrPayload,
          updated_at: new Date().toISOString(),
        })
        .eq("id", existingPayment.id)
        .select()
        .single();

      if (!updateError && updatedPayment) {
        paymentRecord = updatedPayment;
      }
    }

    // 5. Reconcile / advance booking to AWAITING_PAYMENT if currently ARRIVED or STARTED
    const originalStatus = booking.status;
    let statusRepaired = false;
    if (booking.status === "ARRIVED" || booking.status === "STARTED") {
      await supabaseClient
        .from("bookings")
        .update({ status: "AWAITING_PAYMENT" })
        .eq("id", booking.id);
      statusRepaired = true;
    }

    // 6. Record audit log
    const auditDetail = statusRepaired
      ? `Repaired stale status ${originalStatus} -> AWAITING_PAYMENT & generated payment details for ₹${booking.total_amount}`
      : `Generated payment details for ₹${booking.total_amount}`;

    await supabaseClient.from("payment_audit_log").insert({
      payment_id: paymentRecord?.id,
      booking_id: booking.id,
      actor_id: identity.profileId,
      actor_role: (identity.role || "PARTNER").toUpperCase(),
      action: "QR_GENERATED",
      detail: auditDetail,
    });

    return new Response(
      JSON.stringify({
        payment_id: paymentRecord?.id,
        booking_id: booking.id,
        amount: booking.total_amount,
        currency: "INR",
        qr_payload: qrPayload,
        upi_payee_vpa: upiVpa,
        upi_payee_name: upiName,
        status: paymentRecord?.status ?? "PENDING",
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
