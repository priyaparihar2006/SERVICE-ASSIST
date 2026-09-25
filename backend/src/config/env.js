import 'dotenv/config';
import { z } from 'zod';

export const env = z
  .object({
    NODE_ENV: z.enum(['development', 'test', 'production']).default('development'),
    PORT: z.coerce.number().int().min(1).max(65535).default(5000),
    DATABASE_URL: z.string().startsWith('postgresql://'),
    JWT_SECRET: z
      .string()
      .min(32)
      .refine((v) => !v.startsWith('REPLACE_'), 'Generate a random JWT_SECRET'),
    // Exact browser origins allowed to call the API. Optional in development, required in production.
    FRONTEND_ORIGINS: z.string().optional(),
    COOKIE_SAME_SITE: z.enum(['lax', 'strict', 'none']).default('lax'),
    TRUST_PROXY: z.coerce.number().int().min(0).max(3).default(0),
    // Comma-separated "version:base64(32 random bytes)" master keys used to wrap chat data keys.
    CHAT_ENCRYPTION_KEYS: z.string().min(1, 'Set CHAT_ENCRYPTION_KEYS (see .env.example)'),
    CHAT_ACTIVE_KEY_VERSION: z.coerce.number().int().positive(),
    // Reject messages that contain phone numbers or e-mail addresses (best effort).
    CHAT_BLOCK_CONTACT_INFO: z
      .enum(['true', 'false'])
      .default('true')
      .transform((v) => v === 'true'),
  })
  .parse(process.env);
export const chatKeys = parseChatKeys(env.CHAT_ENCRYPTION_KEYS, env.CHAT_ACTIVE_KEY_VERSION);
// The Vite dev server is reachable as both localhost and 127.0.0.1, which are different origins to a
// browser, so development trusts both names for the one dev port. Production has no default.
const DEV_ORIGINS = 'http://localhost:5173,http://127.0.0.1:5173';

export const origins = parseOrigins(env.FRONTEND_ORIGINS, env.NODE_ENV);
export const cookieOptions = {
  httpOnly: true,
  secure: env.NODE_ENV === 'production',
  sameSite: env.COOKIE_SAME_SITE,
  path: '/api',
};

function parseChatKeys(spec, activeVersion) {
  const keys = new Map();
  for (const entry of spec.split(',')) {
    const [version, material, extra] = entry.trim().split(':');
    const key = Buffer.from(material || '', 'base64');
    if (!/^[1-9]\d*$/.test(version || '') || extra !== undefined || key.length !== 32)
      throw new Error('CHAT_ENCRYPTION_KEYS entries must look like 1:<base64 of 32 random bytes>');
    if (keys.has(Number(version))) throw new Error('Duplicate CHAT_ENCRYPTION_KEYS version');
    keys.set(Number(version), key);
  }
  if (!keys.has(activeVersion))
    throw new Error('CHAT_ACTIVE_KEY_VERSION does not match a configured chat key');
  return { keys, activeVersion };
}

function parseOrigins(value, nodeEnv) {
  let list = value?.trim();
  if (!list) {
    if (nodeEnv === 'production')
      throw new Error('Set FRONTEND_ORIGINS to the exact origin(s) of the frontend in production');
    list = DEV_ORIGINS;
  }
  const parsed = new Set();
  for (const raw of list.split(',')) {
    const entry = raw.trim().replace(/\/+$/, '');
    if (!entry) continue;
    if (entry === '*') {
      parsed.add('*');
      continue;
    }
    if (entry.includes('*')) {
      parsed.add(entry);
      continue;
    }
    let url;
    try {
      url = new URL(entry);
    } catch {
      url = null;
    }
    if (
      !url ||
      !['http:', 'https:'].includes(url.protocol) ||
      url.username ||
      url.password ||
      (url.pathname !== '/' && url.pathname !== '') ||
      url.search ||
      url.hash
    )
      throw new Error(
        `FRONTEND_ORIGINS entry "${entry}" must be a valid origin (no path or query)`,
      );
    parsed.add(url.origin); // normalises a trailing slash, letter case and default ports
  }
  if (!parsed.size) throw new Error('FRONTEND_ORIGINS must list at least one origin');
  return [...parsed];
}

export function isAllowedOrigin(origin) {
  if (!origin) return false;
  try {
    const url = new URL(origin);
    const normalized = url.origin;
    for (const allowed of origins) {
      if (allowed === '*' || allowed === normalized) return true;
      if (allowed.includes('*')) {
        const regexStr =
          '^' + allowed.replace(/[.+?^${}()|[\]\\]/g, '\\$&').replace(/\*/g, '.*') + '$';
        if (new RegExp(regexStr).test(normalized)) return true;
      }
      // If any vercel domain or vercel origin is configured, allow Vercel project preview deployments
      if (
        (allowed.includes('.vercel.app') || allowed.includes('serviceassist')) &&
        url.hostname.endsWith('.vercel.app')
      ) {
        return true;
      }
    }
  } catch {
    return false;
  }
  return false;
}

