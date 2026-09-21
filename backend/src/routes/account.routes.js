import { Router } from 'express';
import { z } from 'zod';
import rateLimit from 'express-rate-limit';
import { db } from '../config/db.js';
import { authenticate, authorize, optionalAuthenticate } from '../middleware/auth.js';
import { validate, id, text, pageQuery } from '../middleware/validate.js';
import { endpoint, ensure } from '../utils/errors.js';
import { cartLines, discountFor } from '../services/booking.service.js';
const router = Router();
const address = z.object({
  type: z.enum(['Home', 'Work', 'Other']).default('Home'),
  house: text,
  street: text,
  area: z.string().max(200).default(''),
  city: text,
  state: text,
  pincode: z.string().regex(/^\d{6}$/),
  landmark: z.string().max(200).optional(),
  isDefault: z.boolean().default(false),
  latitude: z.number().min(-90).max(90).optional(),
  longitude: z.number().min(-180).max(180).optional(),
});
router.get(
  '/addresses',
  authenticate,
  endpoint(async (req, res) =>
    res.json({ addresses: await db.address.findMany({ where: { userId: req.user.id } }) }),
  ),
);
router.post(
  ['/addresses', '/auth/addresses'],
  authenticate,
  validate(address),
  endpoint(async (req, res) => {
    const result = await db.$transaction(async (tx) => {
      await tx.$queryRaw`SELECT id FROM "User" WHERE id = ${req.user.id} FOR UPDATE`;
      if (req.validated.isDefault)
        await tx.address.updateMany({ where: { userId: req.user.id }, data: { isDefault: false } });
      return tx.address.create({
        data: {
          ...req.validated,
          userId: req.user.id,
          addressLine: [req.validated.house, req.validated.street, req.validated.area]
            .filter(Boolean)
            .join(', '),
        },
      });
    });
    res.status(201).json({ address: result });
  }),
);
router.put(
  '/addresses/:id/default',
  authenticate,
  endpoint(async (req, res) => {
    await db.$transaction(async (tx) => {
      await tx.$queryRaw`SELECT id FROM "User" WHERE id = ${req.user.id} FOR UPDATE`;
      ensure(
        await tx.address.findFirst({ where: { id: id.parse(req.params.id), userId: req.user.id } }),
        404,
        'Address not found',
      );
      await tx.address.updateMany({ where: { userId: req.user.id }, data: { isDefault: false } });
      await tx.address.update({ where: { id: req.params.id }, data: { isDefault: true } });
    });
    res.json({ success: true });
  }),
);
router.get(
  ['/coupons', '/offers'],
  endpoint(async (req, res) =>
    res.json({
      coupons: await db.offer.findMany({ where: { isActive: true, expiry: { gt: new Date() } } }),
    }),
  ),
);
// Slows down guessing of coupon codes.
const couponLimiter = rateLimit({
  windowMs: 60000,
  limit: 30,
  standardHeaders: 'draft-8',
  legacyHeaders: false,
  handler: (req, res) =>
    res
      .status(429)
      .json({ error: 'Too many coupon attempts. Please wait a minute and try again.' }),
});
const publicCoupon = (c) => ({
  code: c.code,
  description: c.description,
  discountType: c.discountType,
  value: Number(c.value),
  minBookingAmount: Number(c.minBookingAmount),
  maxDiscount: c.maxDiscount == null ? undefined : Number(c.maxDiscount),
  expiry: c.expiry,
  categoryIds: c.categoryIds,
});
// Preview only: the cart total and discount are computed here from database prices. The browser
// sends the code and which items are in the cart, never an amount. Checkout re-validates everything.
router.post(
  '/coupons/validate',
  couponLimiter,
  optionalAuthenticate,
  validate(
    z.object({
      code: text,
      items: z
        .array(
          z.object({
            serviceId: id,
            variantId: id.optional(),
            quantity: z.number().int().min(1).max(10).default(1),
          }),
        )
        .min(1)
        .max(10),
    }),
  ),
  endpoint(async (req, res) => {
    const lines = await cartLines(db, req.validated.items);
    const { coupon, discount, subtotal } = await discountFor(
      db,
      req.validated.code,
      lines,
      req.user?.id,
    );
    res.json({
      valid: true,
      coupon: publicCoupon(coupon),
      subtotal,
      discount,
      taxes: 0,
      total: subtotal - discount,
      message: `${coupon.code} applied: you save \u20B9${discount}. Final pricing is confirmed at checkout.`,
    });
  }),
);
router.get(
  '/notifications',
  authenticate,
  endpoint(async (req, res) => {
    const q = pageQuery.parse(req.query);
    const rows = await db.notification.findMany({
      where: { userId: req.user.id },
      orderBy: { createdAt: 'desc' },
      skip: (q.page - 1) * q.limit,
      take: q.limit,
    });
    res.json({
      notifications: rows.map((n) => ({ ...n, read: n.isRead, timestamp: n.createdAt })),
    });
  }),
);
router.patch(
  '/notifications/:id/read',
  authenticate,
  endpoint(async (req, res) => {
    const result = await db.notification.updateMany({
      where: { id: id.parse(req.params.id), userId: req.user.id },
      data: { isRead: true },
    });
    ensure(result.count, 404, 'Notification not found');
    res.json({ success: true });
  }),
);
router.get(
  '/support/tickets',
  authenticate,
  endpoint(async (req, res) => {
    const q = pageQuery.parse(req.query);
    res.json({
      tickets: await db.supportTicket.findMany({
        where: req.user.role === 'ADMIN' ? {} : { userId: req.user.id },
        orderBy: { createdAt: 'desc' },
        skip: (q.page - 1) * q.limit,
        take: q.limit,
      }),
    });
  }),
);
router.post(
  ['/support/tickets', '/support/ticket'],
  authenticate,
  validate(
    z.object({
      subject: text,
      message: z.string().trim().min(10).max(5000),
      category: text.default('GENERAL'),
    }),
  ),
  endpoint(async (req, res) => {
    const ticket = await db.supportTicket.create({
      data: { ...req.validated, userId: req.user.id },
    });
    res.status(201).json({ ticket, ticketId: ticket.id });
  }),
);
router.get(
  '/reviews',
  endpoint(async (req, res) => {
    const q = pageQuery
      .extend({ serviceId: id.optional(), professionalId: id.optional() })
      .parse(req.query);
    const reviews = await db.review.findMany({
      where: {
        ...(q.professionalId ? { professionalId: q.professionalId } : {}),
        ...(q.serviceId ? { booking: { items: { some: { serviceId: q.serviceId } } } } : {}),
      },
      include: {
        customer: { select: { name: true, profileImage: true } },
        booking: { select: { serviceId: true } },
      },
      orderBy: { createdAt: 'desc' },
      take: q.limit,
      skip: (q.page - 1) * q.limit,
    });
    res.json({
      reviews: reviews.map((r) => ({
        id: r.id,
        bookingId: r.bookingId,
        serviceId: r.booking.serviceId,
        professionalId: r.professionalId,
        customerName: r.customer.name,
        customerAvatar: r.customer.profileImage,
        rating: r.rating,
        comment: r.comment,
        date: r.createdAt,
        tags: [],
        verifiedBooking: true,
      })),
    });
  }),
);
router.post(
  '/reviews',
  authenticate,
  authorize('CUSTOMER'),
  validate(
    z.object({
      bookingId: id,
      rating: z.number().int().min(1).max(5),
      comment: z.string().trim().min(1).max(2000),
    }),
  ),
  endpoint(async (req, res) => {
    const review = await db.$transaction(async (tx) => {
      const b = await tx.booking.findFirst({
        where: { id: req.validated.bookingId, customerId: req.user.id, status: 'COMPLETED' },
        include: { items: true },
      });
      ensure(b?.professionalId, 400, 'Only completed bookings can be reviewed');
      await tx.$queryRaw`SELECT id FROM "Professional" WHERE id = ${b.professionalId} FOR UPDATE`;
      const row = await tx.review.create({
        data: { ...req.validated, customerId: req.user.id, professionalId: b.professionalId },
      });
      const avg = await tx.review.aggregate({
        where: { professionalId: b.professionalId },
        _avg: { rating: true },
      });
      await tx.professional.update({
        where: { id: b.professionalId },
        data: { rating: avg._avg.rating },
      });
      for (const serviceId of [...new Set(b.items.map((i) => i.serviceId))].sort()) {
        await tx.$queryRaw`SELECT id FROM "Service" WHERE id = ${serviceId} FOR UPDATE`;
        const a = await tx.review.aggregate({
          where: { booking: { items: { some: { serviceId } } } },
          _avg: { rating: true },
        });
        await tx.service.update({ where: { id: serviceId }, data: { rating: a._avg.rating } });
      }
      return row;
    });
    res.status(201).json({ review });
  }),
);
export default router;
