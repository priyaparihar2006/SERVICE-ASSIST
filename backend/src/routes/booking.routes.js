import { Router } from 'express';
import { z } from 'zod';
import { db } from '../config/db.js';
import { authenticate, authorize } from '../middleware/auth.js';
import { validate, id, pageQuery } from '../middleware/validate.js';
import { endpoint, ensure } from '../utils/errors.js';
import { bookingInclude, bookingView } from '../utils/serializers.js';
import { createBooking, scope, updateStatus, transitions } from '../services/booking.service.js';
const router = Router();
const item = z
  .object({
    serviceId: id.optional(),
    variantId: id.optional(),
    service: z.object({ id }).optional(),
    variant: z.object({ id }).optional(),
    quantity: z.number().int().min(1).max(10).default(1),
  })
  .refine((v) => v.serviceId || v.service, 'Service is required');
const booking = z
  .object({
    serviceId: id.optional(),
    variantId: id.optional(),
    addressId: id.optional(),
    address: z.object({ id }).optional(),
    items: z.array(item).min(1).max(10).optional(),
    bookingDate: z.iso.date().optional(),
    scheduledDate: z.iso.date().optional(),
    bookingTime: z.string().max(40).optional(),
    scheduledTimeSlot: z.string().max(40).optional(),
    notes: z.string().max(2000).optional(),
    specialInstructions: z.string().max(2000).optional(),
    couponCode: z.string().max(40).optional(),
    paymentMethod: z.enum(['CASH', 'COD']).default('CASH'),
  })
  .refine(
    (v) =>
      (v.serviceId || v.items) &&
      (v.addressId || v.address) &&
      (v.bookingDate || v.scheduledDate) &&
      (v.bookingTime || v.scheduledTimeSlot),
    'Service, address, date and time are required',
  );
router.post(
  '/bookings',
  authenticate,
  authorize('CUSTOMER'),
  validate(booking),
  endpoint(async (req, res) => {
    const requestKey = z.string().uuid().parse(req.get('Idempotency-Key'));
    res
      .status(201)
      .json({
        booking: bookingView(
          await createBooking(req.user, req.validated, requestKey),
          req.user.role,
        ),
      });
  }),
);
router.get(
  ['/bookings', '/professionals/bookings'],
  authenticate,
  endpoint(async (req, res) => {
    if (req.path.startsWith('/professionals'))
      ensure(req.user.role === 'PROFESSIONAL', 403, 'Professional account required');
    const q = pageQuery.parse(req.query),
      where = scope(req.user);
    const [rows, total] = await db.$transaction([
      db.booking.findMany({
        where,
        include: bookingInclude,
        orderBy: { createdAt: 'desc' },
        skip: (q.page - 1) * q.limit,
        take: q.limit,
      }),
      db.booking.count({ where }),
    ]);
    res.json({
      bookings: rows.map((b) => bookingView(b, req.user.role)),
      pagination: { ...q, total, pages: Math.ceil(total / q.limit) },
    });
  }),
);
router.get(
  '/bookings/:id',
  authenticate,
  endpoint(async (req, res) => {
    const b = await db.booking.findFirst({
      where: { id: id.parse(req.params.id), ...scope(req.user) },
      include: bookingInclude,
    });
    ensure(b, 404, 'Booking not found');
    res.json({ booking: bookingView(b, req.user.role) });
  }),
);
const statusSchema = z.object({
  status: z.enum(Object.keys(transitions)),
  professionalId: id.optional(),
  otp: z
    .string()
    .regex(/^\d{4}$/)
    .optional(),
  note: z.string().max(500).optional(),
});
const statusHandler = endpoint(async (req, res) => {
  const b = await updateStatus(req.user, id.parse(req.params.id), req.validated);
  ensure(!b.otpError, 400, 'Incorrect customer verification code');
  res.json({ booking: bookingView(b, req.user.role) });
});
router
  .route('/bookings/:id/status')
  .put(authenticate, validate(statusSchema), statusHandler)
  .patch(authenticate, validate(statusSchema), statusHandler);
router.put(
  '/professionals/bookings/:id/status',
  authenticate,
  authorize('PROFESSIONAL'),
  validate(statusSchema),
  statusHandler,
);
router.put(
  '/bookings/:id/cancel',
  authenticate,
  (req, res, next) => {
    req.validated = { status: 'CANCELLED' };
    next();
  },
  statusHandler,
);
export default router;
