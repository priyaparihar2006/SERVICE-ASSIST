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
    if (!identity.isAdmin) {
      return new Response(JSON.stringify({ error: "Forbidden: Admin access required" }), {
        status: 403,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    const body = await req.json().catch(() => ({}));
    const { booking_id: bookingId, action, note } = body;

    if (!bookingId || !action) {
      return new Response(JSON.stringify({ error: "booking_id and action ('MARK_PAID' | 'MARK_FAILED') are required" }), {
        status: 400,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    const { data: booking, error: bookingError } = await supabaseClient
      .from("bookings")
      .select("id, booking_code, total_amount, status")
      .eq("id", bookingId)
      .single();

    if (bookingError || !booking) {
      return new Response(JSON.stringify({ error: "Booking not found" }), {
        status: 404,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    const nowIso = new Date().toISOString();
    const nowMillis = Date.now();

    if (action === "MARK_PAID") {
      const { data: existingPayment } = await supabaseClient
        .from("payments")
        .select("*")
        .eq("booking_id", booking.id)
        .maybeSingle();

      const method = existingPayment?.method || "CASH";

      const { data: payment, error: paymentError } = await supabaseClient
        .from("payments")
        .upsert(
          {
            booking_id: booking.id,
            amount: booking.total_amount,
            currency: "INR",
            method,
            status: "PAID",
            confirmed_by: identity.profileId,
            confirmed_role: "ADMIN",
            confirmation_note: note || "Admin manual override to PAID",
            paid_at: nowIso,
            updated_at: nowIso,
          },
          { onConflict: "booking_id" }
        )
        .select()
        .single();

      if (paymentError) throw paymentError;

      await supabaseClient
        .from("bookings")
        .update({
          status: "COMPLETED",
          is_paid: true,
          payment_method: method,
          paid_at: nowMillis,
        })
        .eq("id", booking.id);

      await supabaseClient.from("payment_audit_log").insert({
        payment_id: payment.id,
        booking_id: booking.id,
        actor_id: identity.profileId,
        actor_role: "ADMIN",
        action: "ADMIN_OVERRIDE",
        detail: `Admin override MARK_PAID: ${note || "No note provided"}`,
      });

      return new Response(
        JSON.stringify({
          success: true,
          booking_id: booking.id,
          action: "MARK_PAID",
          status: "COMPLETED",
          is_paid: true,
        }),
        {
          status: 200,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        }
      );
    } else if (action === "MARK_FAILED") {
      const { data: payment, error: paymentError } = await supabaseClient
        .from("payments")
        .update({
          status: "FAILED",
          confirmation_note: note || "Admin manual override to FAILED",
          updated_at: nowIso,
        })
        .eq("booking_id", booking.id)
        .select()
        .maybeSingle();

      if (paymentError) throw paymentError;

      await supabaseClient.from("payment_audit_log").insert({
        payment_id: payment?.id,
        booking_id: booking.id,
        actor_id: identity.profileId,
        actor_role: "ADMIN",
        action: "ADMIN_OVERRIDE",
        detail: `Admin override MARK_FAILED: ${note || "No note provided"}`,
      });

      return new Response(
        JSON.stringify({
          success: true,
          booking_id: booking.id,
          action: "MARK_FAILED",
          status: payment?.status || "FAILED",
        }),
        {
          status: 200,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        }
      );
    } else {
      return new Response(JSON.stringify({ error: `Invalid action: ${action}` }), {
        status: 400,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }
  } catch (err: any) {
    return new Response(JSON.stringify({ error: err.message || "Internal server error" }), {
      status: 500,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }
});
