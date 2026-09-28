import { ChatCryptoError } from "./crypto.ts";

export const defaultCorsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type, x-dev-profile-id, x-dev-user-role",
};

export function jsonResponse(data: any, status: number = 200, corsHeaders = defaultCorsHeaders): Response {
  return new Response(JSON.stringify(data), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}

export function jsonError(
  err: any,
  fallbackMessage: string = "Something went wrong, please retry",
  corsHeaders = defaultCorsHeaders,
  stage?: string
): Response {
  if (err instanceof ChatCryptoError) {
    console.error(`[ChatCryptoError ${err.kind}]`, err.message, err.detail, stage ? `stage=${stage}` : "");
    if (err.kind === "MASTER_KEY") {
      return jsonResponse({ error: "Chat service key configuration error", code: "MASTER_KEY", stage }, 500, corsHeaders);
    } else {
      return jsonResponse({ error: "This conversation can't be decrypted. Please start a new chat.", code: "CONVERSATION_KEY", stage }, 409, corsHeaders);
    }
  }

  const rawMsg = err?.message || String(err);
  const errCode = err?.code;
  console.error("[ServerError]", err?.name, rawMsg, err?.stack?.split("\n")?.[1] || "", stage ? `stage=${stage}` : "", err);

  if (
    errCode === "22P02" ||
    errCode === "23502" ||
    errCode === "42804" ||
    errCode === "42P13" ||
    errCode === "PGRST203" ||
    errCode === "42883" ||
    errCode === "PGRST202" ||
    errCode === "42703" ||
    errCode === "42P01" ||
    (rawMsg.includes("function") && rawMsg.includes("does not exist"))
  ) {
    return jsonResponse({ error: "Chat server is being updated. Please try again shortly.", code: "SCHEMA_MISMATCH", stage }, 500, corsHeaders);
  }

  if (rawMsg === "Unauthorized" || rawMsg.includes("authorization") || rawMsg.includes("JWT")) {
    return jsonResponse({ error: "Unauthorized access", code: "UNAUTHORIZED", stage }, 401, corsHeaders);
  }

  if (rawMsg.includes("Forbidden") || rawMsg.includes("not participant") || rawMsg.includes("Not participant")) {
    return jsonResponse({ error: "Forbidden: Access denied", code: "FORBIDDEN", stage }, 403, corsHeaders);
  }

  if (rawMsg.includes("not found") || rawMsg.includes("Not found") || rawMsg.includes("PGRST116")) {
    return jsonResponse({ error: fallbackMessage.includes("found") ? fallbackMessage : "Requested resource not found", code: "NOT_FOUND", stage }, 404, corsHeaders);
  }

  return jsonResponse({ error: fallbackMessage, code: errCode || "INTERNAL", stage }, 500, corsHeaders);
}
