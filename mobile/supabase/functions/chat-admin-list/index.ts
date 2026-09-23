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
    const targetUserId = url.searchParams.get("user_id");
    const bookingId = url.searchParams.get("booking_id");

    // Write immutable audit log
    await supabaseClient.from("chat_admin_audit").insert({
      admin_id: identity.userId,
      target_user_id: targetUserId,
      action: "LIST",
    });

    let query = supabaseClient.from("chat_conversations").select(`
      id,
      booking_id,
      customer_id,
      partner_id,
      status,
      last_message_at,
      last_message_seq,
      bookings (
        booking_code,
        service_name,
        customer_name
      )
    `);

    if (targetUserId) {
      query = query.or(`customer_id.eq.${targetUserId},partner_id.eq.${targetUserId}`);
    } else if (bookingId) {
      query = query.eq("booking_id", bookingId);
    }

    const { data: convs, error } = await query.order("last_message_at", { ascending: false, nullsFirst: false });
    if (error) throw error;

    return new Response(JSON.stringify({ conversations: convs }), {
      status: 200,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  } catch (err: any) {
    const status = err.message === "Unauthorized" || err.message?.includes("authorization") ? 401 : 500;
    return new Response(JSON.stringify({ error: err.message || "Internal server error" }), {
      status,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }
});
