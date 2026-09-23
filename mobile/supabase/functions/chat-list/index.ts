import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.0";
import { getMasterKey, unwrapDek, decryptMessage, hexToBytes } from "../_shared/crypto.ts";
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
    const profileId = identity.profileId;

    // 1. Query conversations
    const { data: convs, error: convError } = await supabaseClient
      .from("chat_conversations")
      .select(`
        id,
        booking_id,
        customer_id,
        partner_id,
        status,
        opened_by,
        opened_at,
        last_message_at,
        last_message_seq,
        wrapped_dek,
        key_version,
        bookings (
          booking_code,
          service_name,
          customer_name
        )
      `)
      .or(`customer_id.eq.${profileId},partner_id.eq.${profileId}`)
      .order("last_message_at", { ascending: false, nullsFirst: false });

    if (convError) throw convError;

    // Filter hidden 0-message conversations
    const visibleConvs = (convs || []).filter((c: any) => {
      if (Number(c.last_message_seq || 0) === 0 && c.opened_by && c.opened_by !== profileId) {
        return false;
      }
      return true;
    });

    if (visibleConvs.length === 0) {
      return new Response(JSON.stringify({ conversations: [] }), {
        status: 200,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    const convIds = visibleConvs.map((c: any) => c.id);

    // 2. Fetch all read states for these conversations in one query
    const { data: readStates } = await supabaseClient
      .from("chat_read_state")
      .select("conversation_id, user_id, last_read_seq, last_delivered_seq")
      .in("conversation_id", convIds);

    const readStateMap = new Map<string, { lastRead: number; lastDelivered: number }>();
    for (const rs of (readStates || [])) {
      readStateMap.set(`${rs.conversation_id}_${rs.user_id}`, {
        lastRead: Number(rs.last_read_seq || 0),
        lastDelivered: Number(rs.last_delivered_seq || 0),
      });
    }

    // 3. Fetch last messages in batch
    const { data: lastMessages } = await supabaseClient
      .from("chat_messages")
      .select("id, conversation_id, seq, ciphertext, nonce, sender_id, created_at, kind")
      .in("conversation_id", convIds)
      .order("seq", { ascending: false });

    const lastMessageByConv = new Map<string, any>();
    for (const msg of (lastMessages || [])) {
      if (!lastMessageByConv.has(msg.conversation_id)) {
        lastMessageByConv.set(msg.conversation_id, msg);
      }
    }

    // 4. Partner names lookup
    const partnerIds = visibleConvs.map((c: any) => c.partner_id);
    const { data: partnerProfiles } = await supabaseClient
      .from("user_profiles")
      .select("id, name")
      .in("id", partnerIds);

    const partnerNameMap = new Map<string, string>();
    for (const p of (partnerProfiles || [])) {
      partnerNameMap.set(p.id, p.name);
    }

    const masterKey = await getMasterKey(Deno.env.get("CHAT_MASTER_KEY"));

    const results = [];
    for (const conv of visibleConvs) {
      const isCustomer = conv.customer_id === profileId;
      const peerId = isCustomer ? conv.partner_id : conv.customer_id;

      let counterpartDisplayName = "";
      if (isCustomer) {
        const partnerFullName = partnerNameMap.get(conv.partner_id) || "Rajesh Sharma";
        const partnerFirstName = partnerFullName.split(" ")[0] || "Pro";
        counterpartDisplayName = `${partnerFirstName} · Your Professional`;
      } else {
        const custFullName = conv.bookings?.customer_name || "Client";
        const custFirstName = custFullName.split(" ")[0] || "Client";
        counterpartDisplayName = `${custFirstName} · Client`;
      }

      const myState = readStateMap.get(`${conv.id}_${profileId}`) || { lastRead: 0, lastDelivered: 0 };
      const peerState = readStateMap.get(`${conv.id}_${peerId}`) || { lastRead: 0, lastDelivered: 0 };

      // Calculate unread count
      const convMessages = (lastMessages || []).filter((m: any) => m.conversation_id === conv.id);
      const unreadCount = convMessages.filter((m: any) => m.seq > myState.lastRead && m.sender_id !== profileId).length;

      // Last message preview decryption
      const lastMsg = lastMessageByConv.get(conv.id);
      let lastMessagePreview = "No messages yet";
      let lastMessageFromMe = false;

      if (lastMsg) {
        lastMessageFromMe = lastMsg.sender_id === profileId;
        try {
          const dek = await unwrapDek(hexToBytes(conv.wrapped_dek), masterKey);
          lastMessagePreview = await decryptMessage(
            hexToBytes(lastMsg.ciphertext),
            hexToBytes(lastMsg.nonce),
            dek,
            conv.id,
            lastMsg.id,
            lastMsg.sender_id,
            conv.key_version
          );
        } catch {
          lastMessagePreview = "Encrypted message";
        }
      }

      const formattedLastMessageAt = conv.last_message_at 
        ? new Date(conv.last_message_at).toISOString() 
        : (conv.opened_at ? new Date(conv.opened_at).toISOString() : null);

      results.push({
        conversation_id: conv.id,
        booking_id: conv.booking_id,
        booking_code: conv.bookings?.booking_code ?? "",
        service_name: conv.bookings?.service_name ?? "Doorstep Service",
        counterpart_display_name: counterpartDisplayName,
        last_message_preview: lastMessagePreview,
        last_message_at: formattedLastMessageAt,
        last_message_from_me: lastMessageFromMe,
        unread_count: unreadCount,
        status: conv.status,
        peer_read_seq: peerState.lastRead,
        peer_delivered_seq: peerState.lastDelivered,
        latest_seq: Number(conv.last_message_seq || 0),
      });
    }

    return new Response(JSON.stringify({ conversations: results }), {
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
