import { z } from 'zod';
export const id = z
  .string()
  .min(1)
  .max(100)
  .regex(/^[a-zA-Z0-9_-]+$/);
export const text = z.string().trim().min(1).max(200);
export const password = z
  .string()
  .min(12)
  .max(72)
  .refine((v) => Buffer.byteLength(v, 'utf8') <= 72, 'Password must be at most 72 bytes');
export const pageQuery = z.object({
  page: z.coerce.number().int().positive().default(1),
  limit: z.coerce.number().int().min(1).max(100).default(20),
});
export const validate =
  (schema, target = 'body') =>
  (req, res, next) => {
    try {
      req.validated = schema.parse(req[target]);
      next();
    } catch (error) {
      next(error);
    }
  };
