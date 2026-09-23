import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.0";
import { resolveIdentity } from "./_shared/identity.ts";

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

    // 1. Authorization check: Customer or Admin
    if (identity.role !== "CUSTOMER" && identity.role !== "ADMIN") {
      return new Response(JSON.stringify({ error: "Only customers and admins can create bookings" }), {
        status: 403,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    const customerId = body.customer_id || identity.profileId;
    if (identity.role === "CUSTOMER" && customerId !== identity.profileId && identity.isProd) {
      return new Response(JSON.stringify({ error: "Cannot create booking for another customer" }), {
        status: 403,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    if (!body.service_name && !body.service_id) {
      return new Response(JSON.stringify({ error: "service_id or service_name is required" }), {
        status: 400,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    const randomCode = "SRV-" + Math.floor(10000 + Math.random() * 90000);
    const bookingCode = body.booking_code || randomCode;
    const nowMillis = Date.now();

    const insertPayload: Record<string, any> = {
      booking_code: bookingCode,
      customer_id: customerId,
      customer_name: body.customer_name || "Customer",
      customer_phone: body.customer_phone || "+91 98765 43210",
      service_id: body.service_id || "service_general",
      service_name: body.service_name || "General Service",
      package_name: body.package_name || "Standard Package",
      scheduled_date: body.scheduled_date || "Today",
      scheduled_time: body.scheduled_time || "02:00 PM",
      address_text: body.address_text || "Agra",
      locality: body.locality || "Taj Nagri",
      city: body.city || "Agra",
      total_amount: body.total_amount || 499,
      discount_amount: body.discount_amount || 0,
      promo_code: body.promo_code || "",
      payment_method: body.payment_method || "Cash after service",
      is_paid: body.is_paid || false,
      status: body.status || "ASSIGNED",
      professional_id: body.professional_id || "pro_rajesh_1",
      start_otp: body.start_otp || String(Math.floor(1000 + Math.random() * 9000)),
      special_notes: body.special_notes || "",
      payment_reference: body.payment_reference || null,
      paid_at: body.paid_at || null,
      created_at: body.created_at || nowMillis,
    };

    if (body.id && typeof body.id === "number" && body.id > 0) {
      insertPayload.id = body.id;
    }

    // Insert or upsert by booking_code
    const { data: insertedBooking, error: insertError } = await supabaseClient
      .from("bookings")
      .upsert(insertPayload, { onConflict: "booking_code" })
      .select()
      .single();

    if (insertError) {
      throw insertError;
    }

    return new Response(JSON.stringify(insertedBooking), {
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
