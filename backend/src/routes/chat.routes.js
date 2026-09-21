import { Router } from 'express';
import { z } from 'zod';
import rateLimit from 'express-rate-limit';
import { authenticate, authorize } from '../middleware/auth.js';
import { validate, id, pageQuery } from '../middleware/validate.js';
import { endpoint } from '../utils/errors.js';
import * as chat from '../services/chat.service.js';
import * as chatAdmin from '../services/chatAdmin.service.js';

const router = Router();
const uuid = z.uuid();
// Limits are keyed by the authenticated user (not the IP), so they follow the account.
const perUser = (limit, windowMs, error) =>
  rateLimit({
    windowMs,
    limit,
    standardHeaders: 'draft-8',
    legacyHeaders: false,
    keyGenerator: (req) => `chat:${req.user.id}`,
    handler: (req, res) => res.status(429).json({ error }),
  });
const sendLimiter = perUser(
  30,
  60000,
  'You are sending messages too quickly. Please wait a moment.',
);
const readLimiter = perUser(240, 60000, 'Too many requests. Please slow down.');
const reportLimiter = perUser(
  5,
  3600000,
  'You have submitted too many reports. Please try again later.',
);
const adminLimiter = perUser(60, 60000, 'Too many requests. Please slow down.');

// Conversation content is private: never let a browser or proxy cache it.
router.use(['/conversations', '/messages', '/admin/chat'], (req, res, next) => {
  res.set('Cache-Control', 'no-store');
  next();
});
router.use(
  ['/conversations', '/messages'],
  authenticate,
  authorize('CUSTOMER', 'PROFESSIONAL'),
  readLimiter,
);

// Control characters and bidirectional overrides can hide or spoof text. The class is built from
// code points so that no formatter can turn it into raw, invisible characters in the source.
const UNSUPPORTED_RANGES = [
  [0x00, 0x08],
  [0x0b, 0x0c],
  [0x0e, 0x1f],
  [0x7f, 0x7f],
  [0x202a, 0x202e],
  [0x2066, 0x2069],
];
const UNSUPPORTED_CHARACTERS = new RegExp(
  `[${UNSUPPORTED_RANGES.map(([a, b]) => String.fromCharCode(a) + '-' + String.fromCharCode(b)).join('')}]`,
);

const messageBody = z.object({
  content: z
    .string()
    .transform((v) => v.replace(/\r\n?/g, '\n').trim())
    .pipe(
      z
        .string()
        .min(1, 'Write a message first')
        .max(2000, 'Messages can be at most 2000 characters')
        .refine((v) => !UNSUPPORTED_CHARACTERS.test(v), 'Message contains unsupported characters'),
    ),
  clientMessageId: uuid,
});

router.get(
  '/conversations',
  endpoint(async (req, res) => {
    const q = pageQuery.parse(req.query);
    const { conversations, total } = await chat.listConversations(req.user, q);
    res.json({ conversations, pagination: { ...q, total, pages: Math.ceil(total / q.limit) } });
  }),
);
router.get(
  '/conversations/unread',
  endpoint(async (req, res) => res.json({ unread: await chat.totalUnread(req.user) })),
);
router.post(
  '/conversations',
  validate(z.object({ bookingId: id })),
  endpoint(async (req, res) => {
    const { conversation, created } = await chat.openConversationForBooking(
      req.user,
      req.validated.bookingId,
    );
    res.status(created ? 201 : 200).json({ conversation });
  }),
);
router.get(
  '/conversations/:id',
  endpoint(async (req, res) =>
    res.json({ conversation: await chat.getConversation(req.user, uuid.parse(req.params.id)) }),
  ),
);
router.get(
  '/conversations/:id/messages',
  endpoint(async (req, res) => {
    const q = z
      .object({
        before: uuid.optional(),
        limit: z.coerce.number().int().min(1).max(100).default(50),
      })
      .parse(req.query);
    res.json(await chat.getMessages(req.user, uuid.parse(req.params.id), q));
  }),
);
router.post(
  '/conversations/:id/messages',
  sendLimiter,
  validate(messageBody),
  endpoint(async (req, res) =>
    res.status(201).json({
      message: await chat.sendMessage(req.user, uuid.parse(req.params.id), req.validated),
    }),
  ),
);
router.put(
  '/conversations/:id/read',
  endpoint(async (req, res) =>
    res.json({ success: true, ...(await chat.markRead(req.user, uuid.parse(req.params.id))) }),
  ),
);
router
  .route('/conversations/:id/block')
  .put(
    endpoint(async (req, res) =>
      res.json({ conversation: await chat.setBlocked(req.user, uuid.parse(req.params.id), true) }),
    ),
  )
  .delete(
    endpoint(async (req, res) =>
      res.json({ conversation: await chat.setBlocked(req.user, uuid.parse(req.params.id), false) }),
    ),
  );
router.post(
  '/conversations/:id/report',
  reportLimiter,
  validate(
    z.object({
      reason: z.enum(['HARASSMENT', 'SPAM', 'OFF_PLATFORM_CONTACT', 'SAFETY', 'OTHER']),
      details: z.string().trim().max(1000).optional(),
    }),
  ),
  endpoint(async (req, res) =>
    res.status(201).json({
      report: await chat.reportConversation(req.user, uuid.parse(req.params.id), req.validated),
    }),
  ),
);
router.delete(
  '/messages/:id',
  endpoint(async (req, res) =>
    res.json(await chat.deleteMessage(req.user, uuid.parse(req.params.id))),
  ),
);

// Administrator safety review: report-gated, justified and audited (see chatAdmin.service.js).
router.use('/admin/chat', authenticate, authorize('ADMIN'), adminLimiter);
router.get(
  '/admin/chat/reports',
  endpoint(async (req, res) => {
    const q = pageQuery
      .extend({ status: z.enum(['OPEN', 'RESOLVED', 'DISMISSED']).optional() })
      .parse(req.query);
    const { reports, total } = await chatAdmin.listReports(q);
    res.json({
      reports,
      pagination: { page: q.page, limit: q.limit, total, pages: Math.ceil(total / q.limit) },
    });
  }),
);
router.post(
  '/admin/chat/reports/:id/messages',
  validate(z.object({ justification: z.string().trim().min(15).max(500) })),
  endpoint(async (req, res) =>
    res.json(
      await chatAdmin.readReportedMessages(
        req.user,
        uuid.parse(req.params.id),
        req.validated.justification,
      ),
    ),
  ),
);
router.put(
  '/admin/chat/reports/:id',
  validate(
    z.object({
      status: z.enum(['RESOLVED', 'DISMISSED']),
      resolutionNote: z.string().trim().min(3).max(1000),
    }),
  ),
  endpoint(async (req, res) =>
    res.json({
      report: await chatAdmin.resolveReport(req.user, uuid.parse(req.params.id), req.validated),
    }),
  ),
);
router.get(
  '/admin/chat/access-log',
  endpoint(async (req, res) => {
    const q = pageQuery.parse(req.query);
    const { entries, total } = await chatAdmin.listAccessLog(q);
    res.json({ entries, pagination: { ...q, total, pages: Math.ceil(total / q.limit) } });
  }),
);
export default router;
