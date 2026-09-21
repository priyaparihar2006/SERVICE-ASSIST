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
  })
  .parse(process.env);
export const origins = env.FRONTEND_ORIGINS.split(',').map((v) => v.trim());
export const cookieOptions = {
  httpOnly: true,
  secure: env.NODE_ENV === 'production',
  sameSite: env.COOKIE_SAME_SITE,
  path: '/api',
};
