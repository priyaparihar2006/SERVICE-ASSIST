import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.0";
import { resolveIdentity } from "../_shared/identity.ts";
import { jsonResponse, jsonError, defaultCorsHeaders } from "../_shared/http.ts";

serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: defaultCorsHeaders });

  try {
    const supabaseClient = createClient(
      Deno.env.get("SUPABASE_URL") ?? "",
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? ""
    );

    const identity = await resolveIdentity(req, supabaseClient);
    if (!identity.isAdmin) {
      return jsonResponse({ error: "Forbidden: Admin access required" }, 403);
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

    return jsonResponse({ conversations: convs || [] });
  } catch (err: any) {
    return jsonError(err, "Failed to list admin conversations");
  }
});
