import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.0";
import { getMasterKey, unwrapDek, encryptMessage, hexToBytes, bytesToHex } from "../_shared/crypto.ts";
import { scrubMessageContent } from "../_shared/scrubber.ts";
import { resolveIdentity } from "../_shared/identity.ts";
import { jsonResponse, jsonError, defaultCorsHeaders } from "../_shared/http.ts";

serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: defaultCorsHeaders });

  let stage = "init";
  try {
    const supabaseClient = createClient(
      Deno.env.get("SUPABASE_URL") ?? "",
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? ""
    );

    stage = "auth";
    const identity = await resolveIdentity(req, supabaseClient);
    const profileId = identity.profileId;

    const body = await req.json().catch(() => ({}));
    const {
      conversation_id,
      client_message_id,
      text,
      kind = "TEXT",
    } = body;

    if (!conversation_id || !client_message_id || !text) {
      return jsonResponse({ error: "Missing required fields" }, 400);
    }

    if (text.length > 1000) {
      return jsonResponse({ error: "Message exceeds maximum length of 1000 characters" }, 400);
    }

    // Fetch conversation
    stage = "load_conv";
    const { data: conv, error: convError } = await supabaseClient
      .from("chat_conversations")
      .select("id, booking_id, customer_id, partner_id, status, wrapped_dek, key_version")
      .eq("id", conversation_id)
      .single();

    if (convError || !conv) {
      return jsonResponse({ error: "Conversation not found" }, 404);
    }

    if (conv.status !== "ACTIVE") {
      return jsonResponse({ error: "This chat is closed." }, 403);
    }

    const isCustomer = conv.customer_id === profileId;
    const isPartner = conv.partner_id === profileId;

    if (!isCustomer && !isPartner) {
      return jsonResponse({ error: "Forbidden: Not participant of this conversation" }, 403);
    }

    const senderRole = isCustomer ? "CUSTOMER" : "PARTNER";

    // 1. Contact Scrubber
    stage = "scrub";
    if (kind === "TEXT") {
      const scrubResult = scrubMessageContent(text);
      if (scrubResult.blocked) {
        await supabaseClient.from("chat_flags").insert({
          conversation_id: conv.id,
          sender_id: profileId,
          reason: scrubResult.reason || "PHONE",
        });

        return jsonResponse(
          {
            error: scrubResult.userMessage || "Contact sharing is restricted. Keep chatting in the app.",
            code: "CONTACT_SHARING_RESTRICTED",
          },
          400
        );
      }
    }

    // 2. Encrypt message
    stage = "crypto";
    const masterKey = await getMasterKey(Deno.env.get("CHAT_MASTER_KEY"));
    const dek = await unwrapDek(hexToBytes(conv.wrapped_dek), masterKey);

    const { ciphertext, nonce } = await encryptMessage(
      text,
      dek,
      conv.id,
      client_message_id,
      profileId,
      conv.key_version || 1
    );

    // 3. Atomically append message via RPC chat_append_message
    stage = "append";
    const { data: appended, error: rpcError } = await supabaseClient.rpc("chat_append_message", {
      p_conv: conv.id,
      p_id: client_message_id,
      p_sender: profileId,
      p_role: senderRole,
      p_kind: kind,
      p_ct: bytesToHex(ciphertext),
      p_nonce: bytesToHex(nonce),
    });

    if (rpcError) {
      if (rpcError.code === "23505") {
        return jsonResponse({ error: "Message ID conflict", code: "MESSAGE_ID_CONFLICT" }, 409);
      }
      throw rpcError;
    }

    const row = Array.isArray(appended) ? appended[0] : appended;
    if (!row?.id) throw new Error("append returned no row");

    stage = "respond";
    return jsonResponse({
      success: true,
      message_id: row.id,
      seq: Number(row.seq),
      created_at: new Date(row.created_at).toISOString(),
    });
  } catch (err: any) {
    return jsonError(err, "Failed to send message", defaultCorsHeaders, stage);
  }
});
