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
    FRONTEND_ORIGINS: z.string().default('http://localhost:5173'),
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
export const origins = env.FRONTEND_ORIGINS.split(',').map((v) => v.trim());
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
