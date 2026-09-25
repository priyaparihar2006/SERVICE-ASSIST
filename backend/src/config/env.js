import 'dotenv/config';
import { z } from 'zod';

const DEFAULT_DEV_CHAT_KEY = '1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=';

export const env = z
  .object({
    NODE_ENV: z.enum(['development', 'test', 'production']).default('development'),
    PORT: z.coerce.number().int().min(1).max(65535).default(5000),
    DATABASE_URL: z
      .string()
      .refine(
        (v) => v.startsWith('postgresql://') || v.startsWith('postgres://'),
        'DATABASE_URL must be a PostgreSQL connection string (starting with postgresql:// or postgres://)',
      ),
    JWT_SECRET: z
      .string()
      .min(16)
      .default('dev_jwt_secret_must_be_at_least_32_characters_long_for_security'),
    // Exact browser origins allowed to call the API.
    FRONTEND_ORIGINS: z
      .string()
      .optional()
      .default(() => process.env.FRONTEND_URL || process.env.CORS_ORIGIN || ''),
    COOKIE_SAME_SITE: z.enum(['lax', 'strict', 'none']).optional(),
    TRUST_PROXY: z.coerce.number().int().min(0).max(5).default(1),
    // Comma-separated "version:base64(32 random bytes)" master keys used to wrap chat data keys.
    CHAT_ENCRYPTION_KEYS: z.string().default(DEFAULT_DEV_CHAT_KEY),
    CHAT_ACTIVE_KEY_VERSION: z.coerce.number().int().positive().default(1),
    // Reject messages that contain phone numbers or e-mail addresses (best effort).
    CHAT_BLOCK_CONTACT_INFO: z
      .enum(['true', 'false'])
      .default('true')
      .transform((v) => v === 'true'),
  })
  .parse({
    ...process.env,
    FRONTEND_ORIGINS: process.env.FRONTEND_ORIGINS || process.env.FRONTEND_URL || process.env.CORS_ORIGIN,
  });

export const chatKeys = parseChatKeys(
  env.CHAT_ENCRYPTION_KEYS || DEFAULT_DEV_CHAT_KEY,
  env.CHAT_ACTIVE_KEY_VERSION || 1,
);

// Common development origins
const DEV_ORIGINS = [
  'http://localhost:5173',
  'http://127.0.0.1:5173',
  'http://localhost:3000',
  'http://127.0.0.1:3000',
  'http://localhost:5000',
  'http://127.0.0.1:5000',
  'http://localhost:4173',
  'http://127.0.0.1:4173',
];

export const origins = parseOrigins(env.FRONTEND_ORIGINS, env.NODE_ENV);

const sameSiteSetting =
  env.COOKIE_SAME_SITE || (env.NODE_ENV === 'production' ? 'none' : 'lax');

export const cookieOptions = {
  httpOnly: true,
  secure: env.NODE_ENV === 'production' || sameSiteSetting === 'none',
  sameSite: sameSiteSetting,
  path: '/',
};

function parseChatKeys(spec, activeVersion) {
  const keys = new Map();
  const rawEntries = (spec || DEFAULT_DEV_CHAT_KEY).split(',');
  for (const entry of rawEntries) {
    const [version, material, extra] = entry.trim().split(':');
    let key;
    try {
      key = Buffer.from(material || '', 'base64');
    } catch {
      key = Buffer.alloc(32);
    }
    if (!/^[1-9]\d*$/.test(version || '') || extra !== undefined || key.length !== 32) {
      if (process.env.NODE_ENV === 'production') {
        throw new Error('CHAT_ENCRYPTION_KEYS entries must look like 1:<base64 of 32 random bytes>');
      }
      key = Buffer.alloc(32);
    }
    const numVer = Number(version) || 1;
    keys.set(numVer, key);
  }
  if (!keys.has(activeVersion)) {
    if (process.env.NODE_ENV === 'production') {
      throw new Error('CHAT_ACTIVE_KEY_VERSION does not match a configured chat key');
    }
    keys.set(activeVersion, Buffer.alloc(32));
  }
  return { keys, activeVersion };
}

function parseOrigins(value, nodeEnv) {
  const list = value?.trim();
  const parsed = new Set(DEV_ORIGINS);

  if (list) {
    for (const raw of list.split(',')) {
      const entry = raw.trim().replace(/\/+$/, '');
      if (!entry) continue;
      if (entry === '*' || entry.includes('*')) {
        parsed.add(entry);
        continue;
      }
      try {
        const url = new URL(entry);
        parsed.add(url.origin);
      } catch {
        parsed.add(entry);
      }
    }
  }

  return [...parsed];
}

export function isAllowedOrigin(origin) {
  if (!origin) return true;
  try {
    const url = new URL(origin);
    const normalized = url.origin;

    // Allow localhost / 127.0.0.1 in all environments for local testing
    if (url.hostname === 'localhost' || url.hostname === '127.0.0.1') {
      return true;
    }

    // Always allow Vercel deployment and preview URLs
    if (url.hostname.endsWith('.vercel.app')) {
      return true;
    }

    for (const allowed of origins) {
      if (allowed === '*' || allowed === normalized) return true;
      if (allowed.includes('*')) {
        const regexStr =
          '^' + allowed.replace(/[.+?^${}()|[\]\\]/g, '\\$&').replace(/\*/g, '.*') + '$';
        if (new RegExp(regexStr).test(normalized)) return true;
      }
    }
  } catch {
    return false;
  }
  return false;
}
