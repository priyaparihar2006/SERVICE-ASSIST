import { createCipheriv, createDecipheriv, randomBytes } from 'node:crypto';
import { chatKeys } from '../config/env.js';

// Envelope encryption for chat text (NOT end-to-end: the server can decrypt).
//
//   master key (env / secret manager, versioned)
//     -> wraps a random 256-bit data key per conversation (stored in Conversation.wrappedKey)
//       -> encrypts each message with AES-256-GCM and a fresh random 96-bit IV
//
// Every ciphertext is bound by GCM additional authenticated data to the row it belongs to, so a
// blob copied to another message, conversation or sender fails authentication instead of
// decrypting. Blob layout: iv (12) || auth tag (16) || ciphertext.
const ALGORITHM = 'aes-256-gcm';
const IV_BYTES = 12;
const TAG_BYTES = 16;
export const MESSAGE_ENCRYPTION_VERSION = 1;

function seal(key, plaintext, aad) {
  const iv = randomBytes(IV_BYTES);
  const cipher = createCipheriv(ALGORITHM, key, iv, { authTagLength: TAG_BYTES });
  cipher.setAAD(Buffer.from(aad, 'utf8'));
  const ciphertext = Buffer.concat([cipher.update(plaintext), cipher.final()]);
  return Buffer.concat([iv, cipher.getAuthTag(), ciphertext]);
}

function open(key, blob, aad) {
  if (!blob || blob.length <= IV_BYTES + TAG_BYTES) throw new Error('Malformed ciphertext');
  const decipher = createDecipheriv(ALGORITHM, key, blob.subarray(0, IV_BYTES), {
    authTagLength: TAG_BYTES,
  });
  decipher.setAAD(Buffer.from(aad, 'utf8'));
  decipher.setAuthTag(blob.subarray(IV_BYTES, IV_BYTES + TAG_BYTES));
  return Buffer.concat([decipher.update(blob.subarray(IV_BYTES + TAG_BYTES)), decipher.final()]);
}

const masterKey = (version) => {
  const key = chatKeys.keys.get(version);
  if (!key) throw new Error(`Chat master key version ${version} is not configured`);
  return key;
};
const keyAad = (conversationId, keyVersion) => `dek|${conversationId}|${keyVersion}`;
const messageAad = (version, { conversationId, messageId, senderId }) =>
  `msg|v${version}|${conversationId}|${messageId}|${senderId}`;

/** Create the data key for a new conversation. Store `wrappedKey`/`keyVersion`; keep `dataKey` in memory only. */
export function createConversationKey(conversationId) {
  const dataKey = randomBytes(32);
  const keyVersion = chatKeys.activeVersion;
  const wrappedKey = seal(masterKey(keyVersion), dataKey, keyAad(conversationId, keyVersion));
  return { dataKey, wrappedKey, keyVersion };
}

export const unwrapConversationKey = (conversation) =>
  open(
    masterKey(conversation.keyVersion),
    Buffer.from(conversation.wrappedKey),
    keyAad(conversation.id, conversation.keyVersion),
  );

/** Re-wrap a conversation's data key under the active master key (used by key rotation). */
export function rewrapConversationKey(conversation) {
  const dataKey = unwrapConversationKey(conversation);
  const keyVersion = chatKeys.activeVersion;
  return {
    keyVersion,
    wrappedKey: seal(masterKey(keyVersion), dataKey, keyAad(conversation.id, keyVersion)),
  };
}

export const encryptMessage = (dataKey, context, text) =>
  seal(dataKey, Buffer.from(text, 'utf8'), messageAad(MESSAGE_ENCRYPTION_VERSION, context));

export function decryptMessage(dataKey, context, blob, version = MESSAGE_ENCRYPTION_VERSION) {
  if (version !== MESSAGE_ENCRYPTION_VERSION)
    throw new Error(`Unsupported message encryption version ${version}`);
  return open(dataKey, Buffer.from(blob), messageAad(version, context)).toString('utf8');
}
