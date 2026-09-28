// =========================================================================
// ENVELOPE ENCRYPTION (AES-256-GCM) VIA WEB CRYPTO API
// =========================================================================

const textEncoder = new TextEncoder();
const textDecoder = new TextDecoder();

export class ChatCryptoError extends Error {
  kind: "MASTER_KEY" | "CONVERSATION_KEY";
  detail?: any;

  constructor(kind: "MASTER_KEY" | "CONVERSATION_KEY", message: string, detail?: any) {
    super(message);
    this.name = "ChatCryptoError";
    this.kind = kind;
    this.detail = detail;
  }
}

let cachedMasterKey: CryptoKey | null = null;
let cachedKeyStr: string | null = null;

export async function getMasterKey(base64MasterKey?: string | null): Promise<CryptoKey> {
  const keyStr = base64MasterKey || Deno.env.get("CHAT_MASTER_KEY");
  if (!keyStr) {
    throw new ChatCryptoError("MASTER_KEY", "CHAT_MASTER_KEY is required and not configured");
  }

  if (cachedMasterKey && cachedKeyStr === keyStr) {
    return cachedMasterKey;
  }

  try {
    const binaryString = atob(keyStr);
    if (binaryString.length !== 32) {
      throw new Error(`Invalid master key length: expected 32 bytes, got ${binaryString.length}`);
    }
    const keyBytes = new Uint8Array(32);
    for (let i = 0; i < 32; i++) {
      keyBytes[i] = binaryString.charCodeAt(i);
    }

    const key = await crypto.subtle.importKey(
      "raw",
      keyBytes,
      { name: "AES-GCM" },
      false,
      ["wrapKey", "unwrapKey", "encrypt", "decrypt"]
    );
    cachedMasterKey = key;
    cachedKeyStr = keyStr;
    return key;
  } catch (err: any) {
    throw new ChatCryptoError("MASTER_KEY", "CHAT_MASTER_KEY must be 32 random bytes, base64-encoded", err);
  }
}

/**
 * Generate a fresh random 256-bit DEK (Data Encryption Key) and wrap it with the master key
 */
export async function generateAndWrapDek(masterKey: CryptoKey): Promise<{ dek: CryptoKey; wrappedDek: Uint8Array }> {
  try {
    const dek = await crypto.subtle.generateKey(
      { name: "AES-GCM", length: 256 },
      true,
      ["encrypt", "decrypt"]
    );

    const iv = crypto.getRandomValues(new Uint8Array(12)); // 96-bit IV
    const wrappedRaw = await crypto.subtle.wrapKey(
      "raw",
      dek,
      masterKey,
      { name: "AES-GCM", iv }
    );

    // Store IV + wrapped key together
    const combined = new Uint8Array(iv.length + wrappedRaw.byteLength);
    combined.set(iv, 0);
    combined.set(new Uint8Array(wrappedRaw), iv.length);

    return { dek, wrappedDek: combined };
  } catch (err: any) {
    throw new ChatCryptoError("CONVERSATION_KEY", "Failed to generate conversation encryption key", err);
  }
}

/**
 * Unwrap a stored DEK using the master key
 */
export async function unwrapDek(wrappedDekBytes: Uint8Array, masterKey: CryptoKey): Promise<CryptoKey> {
  if (!wrappedDekBytes || wrappedDekBytes.length < 13) {
    throw new ChatCryptoError("CONVERSATION_KEY", "Invalid wrapped DEK length");
  }
  try {
    const iv = wrappedDekBytes.slice(0, 12);
    const encryptedKey = wrappedDekBytes.slice(12);

    return await crypto.subtle.unwrapKey(
      "raw",
      encryptedKey,
      masterKey,
      { name: "AES-GCM", iv },
      { name: "AES-GCM" },
      false,
      ["encrypt", "decrypt"]
    );
  } catch (err: any) {
    throw new ChatCryptoError("CONVERSATION_KEY", "Failed to unwrap conversation encryption key", err);
  }
}

/**
 * Encrypt a plaintext message with the conversation DEK using AES-256-GCM and AAD
 */
export async function encryptMessage(
  plaintext: string,
  dek: CryptoKey,
  conversationId: string,
  messageId: string,
  senderId: string,
  keyVersion: number
): Promise<{ ciphertext: Uint8Array; nonce: Uint8Array }> {
  const nonce = crypto.getRandomValues(new Uint8Array(12)); // 96-bit nonce
  const aad = textEncoder.encode(`${conversationId}|${messageId}|${senderId}|${keyVersion}`);
  const encodedPlaintext = textEncoder.encode(plaintext);

  const encryptedBuffer = await crypto.subtle.encrypt(
    {
      name: "AES-GCM",
      iv: nonce,
      additionalData: aad
    },
    dek,
    encodedPlaintext
  );

  return {
    ciphertext: new Uint8Array(encryptedBuffer),
    nonce
  };
}

/**
 * Decrypt a ciphertext message with the conversation DEK using AES-256-GCM and AAD
 */
export async function decryptMessage(
  ciphertext: Uint8Array,
  nonce: Uint8Array,
  dek: CryptoKey,
  conversationId: string,
  messageId: string,
  senderId: string,
  keyVersion: number
): Promise<string> {
  const aad = textEncoder.encode(`${conversationId}|${messageId}|${senderId}|${keyVersion}`);

  const decryptedBuffer = await crypto.subtle.decrypt(
    {
      name: "AES-GCM",
      iv: nonce,
      additionalData: aad
    },
    dek,
    ciphertext
  );

  return textDecoder.decode(decryptedBuffer);
}

/**
 * Helper to encode Uint8Array to hex string for Postgres BYTEA
 */
export function bytesToHex(bytes: Uint8Array): string {
  return "\\x" + Array.from(bytes).map(b => b.toString(16).padStart(2, '0')).join('');
}

/**
 * Helper to decode Postgres BYTEA hex or buffer to Uint8Array
 */
export function hexToBytes(hex: string | Uint8Array | null | undefined): Uint8Array {
  if (!hex) return new Uint8Array(0);
  if (hex instanceof Uint8Array) return hex;
  if (typeof hex !== "string") return new Uint8Array(0);
  const cleanHex = hex.startsWith('\\x') ? hex.slice(2) : hex;
  if (cleanHex.length === 0 || cleanHex.length % 2 !== 0) return new Uint8Array(0);
  const bytes = new Uint8Array(cleanHex.length / 2);
  for (let i = 0; i < bytes.length; i++) {
    bytes[i] = parseInt(cleanHex.substr(i * 2, 2), 16);
  }
  return bytes;
}
