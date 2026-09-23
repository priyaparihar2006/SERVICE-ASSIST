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

    const url = new URL(req.url);
    const bookingId = url.searchParams.get("booking_id");
    const limit = parseInt(url.searchParams.get("limit") || "50", 10);
    const offset = parseInt(url.searchParams.get("offset") || "0", 10);

    let query = supabaseClient
      .from("payments")
      .select(`
        id,
        booking_id,
        amount,
        currency,
        method,
        status,
        upi_vpa,
        upi_reference,
        qr_payload,
        confirmed_by,
        confirmed_role,
        confirmation_note,
        created_at,
        updated_at,
        paid_at,
        bookings (
          booking_code,
          service_name,
          customer_name,
          professional_id,
          status
        )
      `)
      .order("created_at", { ascending: false })
      .range(offset, offset + limit - 1);

    if (bookingId) {
      query = query.eq("booking_id", bookingId);
    }

    const { data: payments, error: paymentsError } = await query;
    if (paymentsError) throw paymentsError;

    // Fetch related audit logs if booking_id is provided
    let auditLogs: any[] = [];
    if (bookingId) {
      const { data: logs } = await supabaseClient
        .from("payment_audit_log")
        .select("*")
        .eq("booking_id", bookingId)
        .order("created_at", { ascending: false });
      auditLogs = logs || [];
    }

    return new Response(
      JSON.stringify({
        payments: payments || [],
        audit_logs: auditLogs,
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
