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

    const bookingId = body.booking_id ?? body.id;
    const bookingCode = body.booking_code;
    const newStatus = body.status;

    if (!newStatus) {
      return new Response(JSON.stringify({ error: "status is required" }), {
        status: 400,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    if (!bookingId && !bookingCode) {
      return new Response(JSON.stringify({ error: "booking_id or booking_code is required" }), {
        status: 400,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
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
      return new Response(JSON.stringify({ error: "Booking not found" }), {
        status: 404,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    // Role-based validation
    if (identity.role === "CUSTOMER") {
      if (newStatus.toUpperCase() !== "CANCELLED") {
        return new Response(JSON.stringify({ error: "Customers can only cancel bookings" }), {
          status: 403,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        });
      }
      if (identity.isProd && booking.customer_id !== identity.profileId) {
        return new Response(JSON.stringify({ error: "Cannot cancel a booking belonging to another customer" }), {
          status: 403,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        });
      }
    } else if (identity.role === "PARTNER") {
      if (identity.isProd && booking.professional_id && booking.professional_id !== identity.profileId) {
        return new Response(JSON.stringify({ error: "Partner is not assigned to this booking" }), {
          status: 403,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        });
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

    const { data: updatedBooking, error: updateError } = await supabaseClient
      .from("bookings")
      .update(updatePayload)
      .eq("id", booking.id)
      .select()
      .single();

    if (updateError) {
      throw updateError;
    }

    return new Response(JSON.stringify(updatedBooking), {
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
