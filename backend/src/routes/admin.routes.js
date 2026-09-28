import { Router } from 'express';
import { z } from 'zod';
import { db } from '../config/db.js';
import { authenticate, authorize } from '../middleware/auth.js';
import { validate, text, id, pageQuery } from '../middleware/validate.js';
import { endpoint, ensure } from '../utils/errors.js';
import {
  professionalInclude,
  professionalView,
  publicUser,
  serviceInclude,
  serviceView,
} from '../utils/serializers.js';
const router = Router();
router.use('/admin', authenticate, authorize('ADMIN'));
router.get(
  '/admin/services',
  endpoint(async (req, res) => {
    const q = pageQuery.parse(req.query);
    const [rows, total] = await db.$transaction([
      db.service.findMany({
        include: serviceInclude,
        skip: (q.page - 1) * q.limit,
        take: q.limit,
        orderBy: { id: 'asc' },
      }),
      db.service.count(),
    ]);
    res.json({
      services: rows.map(serviceView),
      pagination: { ...q, total, pages: Math.ceil(total / q.limit) },
    });
  }),
);
router.get(
  '/admin/metrics',
  endpoint(async (req, res) => {
    const [totalUsers, totalProfessionals, totalServices, totalBookings, revenue, ratings] =
      await Promise.all([
        db.user.count(),
        db.professional.count(),
        db.service.count({ where: { isActive: true } }),
        db.booking.count(),
        db.payment.aggregate({ where: { status: 'PAID' }, _sum: { amount: true } }),
        db.review.aggregate({ _avg: { rating: true } }),
      ]);
    res.json({
      totalUsers,
      totalProfessionals,
      totalServices,
      totalBookings,
      revenue: Number(revenue._sum.amount || 0),
      rating: ratings._avg.rating || 0,
    });
  }),
);
router.get(
  '/admin/users',
  endpoint(async (req, res) => {
    const q = pageQuery.parse(req.query);
    res.json({
      users: (
        await db.user.findMany({
          skip: (q.page - 1) * q.limit,
          take: q.limit,
          orderBy: { createdAt: 'desc' },
        })
      ).map(publicUser),
    });
  }),
);
router.get(
  '/admin/professionals',
  endpoint(async (req, res) => {
    const q = pageQuery.parse(req.query);
    res.json({
      professionals: (
        await db.professional.findMany({
          include: professionalInclude,
          take: q.limit,
          skip: (q.page - 1) * q.limit,
        })
      ).map(professionalView),
    });
  }),
);
router.put(
  '/admin/professionals/:id/verification',
  validate(z.object({ verificationStatus: z.enum(['PENDING', 'VERIFIED', 'REJECTED']) })),
  endpoint(async (req, res) =>
    res.json({
      professional: professionalView(
        await db.professional.update({
          where: { id: id.parse(req.params.id) },
          data: req.validated,
          include: professionalInclude,
        }),
      ),
    }),
  ),
);
const category = z.object({
  name: text,
  slug: id,
  description: z.string().min(5).max(2000),
  image: z.url(),
  isActive: z.boolean().optional(),
});
router.get(
  '/admin/categories',
  endpoint(async (req, res) =>
    res.json({ categories: await db.category.findMany({ orderBy: { name: 'asc' } }) }),
  ),
);
router.post(
  '/admin/categories',
  validate(category),
  endpoint(async (req, res) =>
    res.status(201).json({ category: await db.category.create({ data: req.validated }) }),
  ),
);
router.put(
  '/admin/categories/:id',
  validate(category.partial()),
  endpoint(async (req, res) =>
    res.json({
      category: await db.category.update({
        where: { id: id.parse(req.params.id) },
        data: req.validated,
      }),
    }),
  ),
);
router.delete(
  '/admin/categories/:id',
  endpoint(async (req, res) => {
    await db.category.update({ where: { id: id.parse(req.params.id) }, data: { isActive: false } });
    res.json({ success: true });
  }),
);
router.put(
  '/admin/bookings/:id/payment',
  validate(z.object({ transactionId: text })),
  endpoint(async (req, res) => {
    const payment = await db.$transaction(async (tx) => {
      const bookingId = id.parse(req.params.id);
      await tx.$queryRaw`SELECT id FROM "Booking" WHERE id = ${bookingId} FOR UPDATE`;
      const b = await tx.booking.findUnique({
        where: { id: bookingId },
        include: { payment: true },
      });
      ensure(
        b?.status === 'COMPLETED' && b.payment?.paymentMethod === 'CASH',
        409,
        'Only completed cash bookings can be reconciled',
      );
      ensure(b.payment.status === 'PENDING', 409, 'Payment has already been reconciled');
      return tx.payment.update({
        where: { bookingId },
        data: { status: 'PAID', transactionId: req.validated.transactionId },
      });
    });
    res.json({ payment });
  }),
);
router.put(
  '/admin/support/tickets/:id',
  validate(z.object({ status: z.enum(['OPEN', 'IN_PROGRESS', 'RESOLVED']) })),
  endpoint(async (req, res) =>
    res.json({
      ticket: await db.supportTicket.update({
        where: { id: id.parse(req.params.id) },
        data: req.validated,
      }),
    }),
  ),
);
export default router;
