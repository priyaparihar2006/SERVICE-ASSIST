import { Router } from 'express';
import { z } from 'zod';
import rateLimit from 'express-rate-limit';
import { validate, text, password } from '../middleware/validate.js';
import { authenticate } from '../middleware/auth.js';
import { endpoint } from '../utils/errors.js';
import * as c from '../controllers/auth.controller.js';
const router = Router();
const limiter = rateLimit({
  windowMs: 15 * 60000,
  limit: 30,
  standardHeaders: 'draft-8',
  legacyHeaders: false,
});
const registration = z.object({
  name: text,
  email: z.email().max(254),
  phone: z.string().max(25).optional(),
  password,
  role: z.enum(['CUSTOMER', 'PROFESSIONAL']).default('CUSTOMER'),
  businessName: text.optional(),
});
router.post(
  ['/auth/register', '/professionals/register'],
  limiter,
  validate(registration),
  endpoint(c.register),
);
router.post(
  '/auth/login',
  limiter,
  validate(z.object({ email: z.email().max(254), password: z.string().min(1).max(72) })),
  endpoint(c.login),
);
router.get('/auth/me', authenticate, endpoint(c.me));
router.post('/auth/logout', authenticate, endpoint(c.logout));
router.put(
  '/auth/profile',
  authenticate,
  validate(
    z.object({
      name: text.optional(),
      phone: z.string().max(25).optional(),
      profileImage: z.url().max(2048).optional(),
    }),
  ),
  endpoint(c.profile),
);
router.post(
  '/auth/change-password',
  authenticate,
  limiter,
  validate(z.object({ currentPassword: z.string().max(72), password })),
  endpoint(c.changePassword),
);
export default router;
