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
    if (identity.isAdmin) {
      return jsonResponse({ error: "Forbidden: Admin cannot call chat-sync" }, 403);
    }

    let conversations: any[] = [];
    try {
      // Call RPC chat_list_summaries
      const { data: summaryRows, error: rpcError } = await supabaseClient.rpc("chat_list_summaries", {
        p_profile_id: identity.profileId,
      });

      if (!rpcError && summaryRows) {
        conversations = summaryRows.map((c: any) => ({
          conversation_id: c.conversation_id,
          booking_id: Number(c.booking_id || 0),
          status: c.status || "ACTIVE",
          last_message_at: c.last_message_at ? new Date(c.last_message_at).toISOString() : null,
          latest_seq: Number(c.latest_seq || 0),
          unread_count: Number(c.unread_count || 0),
          peer_read_seq: Number(c.peer_last_read || 0),
          peer_delivered_seq: Number(c.peer_last_delivered || 0),
        }));
      } else {
        throw rpcError || new Error("No summaries returned");
      }
    } catch (_fallbackErr) {
      // Direct query fallback for maximum reliability
      const { data: directConvs } = await supabaseClient
        .from("chat_conversations")
        .select("id, booking_id, status, last_message_at, last_message_seq, customer_id, partner_id")
        .or(`customer_id.eq.${identity.profileId},partner_id.eq.${identity.profileId}`)
        .neq("status", "CLOSED");

      if (directConvs) {
        const convIds = directConvs.map((c: any) => c.id);
        const { data: readStates } = await supabaseClient
          .from("chat_read_state")
          .select("conversation_id, user_id, last_read_seq, last_delivered_seq")
          .in("conversation_id", convIds);

        conversations = directConvs.map((c: any) => {
          const peerId = c.customer_id === identity.profileId ? c.partner_id : c.customer_id;
          const peerState = (readStates || []).find((r: any) => r.conversation_id === c.id && r.user_id === peerId);
          const myState = (readStates || []).find((r: any) => r.conversation_id === c.id && r.user_id === identity.profileId);
          const myLastRead = Number(myState?.last_read_seq || 0);
          const latestSeq = Number(c.last_message_seq || 0);
          const unreadCount = Math.max(0, latestSeq - myLastRead);

          return {
            conversation_id: c.id,
            booking_id: Number(c.booking_id || 0),
            status: c.status || "ACTIVE",
            last_message_at: c.last_message_at ? new Date(c.last_message_at).toISOString() : null,
            latest_seq: latestSeq,
            unread_count: unreadCount,
            peer_read_seq: Number(peerState?.last_read_seq || 0),
            peer_delivered_seq: Number(peerState?.last_delivered_seq || 0),
          };
        });
      }
    }

    // Filter conversations that need delivery state updates and update them in bulk
    const updates = conversations
      .filter((c: any) => c.latest_seq && Number(c.latest_seq) > 0)
      .map((c: any) => ({
        conversation_id: c.conversation_id,
        user_id: identity.profileId,
        last_delivered_seq: Number(c.latest_seq),
        updated_at: new Date().toISOString(),
      }));

    if (updates.length > 0) {
      await supabaseClient
        .from("chat_read_state")
        .upsert(updates, { onConflict: "conversation_id, user_id" });
    }

    const totalUnread = conversations.reduce((sum: number, c: any) => sum + Number(c.unread_count || 0), 0);

    return jsonResponse({
      conversations,
      total_unread: totalUnread,
      server_time: new Date().toISOString(),
    });
  } catch (err: any) {
    return jsonError(err, "Failed to sync chat");
  }
});
