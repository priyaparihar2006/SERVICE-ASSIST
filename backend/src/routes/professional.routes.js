import { Router } from 'express';
import { z } from 'zod';
import { db } from '../config/db.js';
import { authenticate, authorize } from '../middleware/auth.js';
import { validate, id, text, pageQuery } from '../middleware/validate.js';
import { endpoint, ensure } from '../utils/errors.js';
import { professionalInclude, professionalView } from '../utils/serializers.js';
const router = Router();
router.get(
  '/professionals',
  endpoint(async (req, res) => {
    const q = pageQuery.extend({ categoryId: id.optional() }).parse(req.query);
    const where = {
      verificationStatus: 'VERIFIED',
      ...(q.categoryId ? { services: { some: { categoryId: q.categoryId } } } : {}),
    };
    const [rows, total] = await db.$transaction([
      db.professional.findMany({
        where,
        include: professionalInclude,
        skip: (q.page - 1) * q.limit,
        take: q.limit,
        orderBy: { id: 'asc' },
      }),
      db.professional.count({ where }),
    ]);
    res.json({
      professionals: rows.map(professionalView),
      pagination: { ...q, total, pages: Math.ceil(total / q.limit) },
    });
  }),
);
router.get(
  '/professionals/profile',
  authenticate,
  authorize('PROFESSIONAL'),
  endpoint(async (req, res) =>
    res.json({
      professional: professionalView(
        await db.professional.findUniqueOrThrow({
          where: { userId: req.user.id },
          include: professionalInclude,
        }),
      ),
    }),
  ),
);
router.put(
  '/professionals/profile',
  authenticate,
  authorize('PROFESSIONAL'),
  validate(
    z.object({
      businessName: text.optional(),
      description: z.string().max(2000).optional(),
      experience: z.number().int().min(0).max(80).optional(),
      serviceArea: z.array(text).max(30).optional(),
      serviceIds: z.array(id).max(50).optional(),
      isAvailableToday: z.boolean().optional(),
    }),
  ),
  endpoint(async (req, res) => {
    const { serviceIds, ...data } = req.validated;
    const current = serviceIds
      ? await db.professional.findUniqueOrThrow({
          where: { userId: req.user.id },
          select: { services: { select: { id: true } } },
        })
      : null;
    const changed =
      serviceIds &&
      JSON.stringify([...new Set(serviceIds)].sort()) !==
        JSON.stringify(current.services.map((s) => s.id).sort());
    if (serviceIds)
      ensure(
        (await db.service.count({
          where: { id: { in: [...new Set(serviceIds)] }, isActive: true },
        })) === new Set(serviceIds).size,
        400,
        'Invalid service selection',
      );
    res.json({
      professional: professionalView(
        await db.professional.update({
          where: { userId: req.user.id },
          data: {
            ...data,
            ...(serviceIds
              ? {
                  services: { set: serviceIds.map((id) => ({ id })) },
                  ...(changed ? { verificationStatus: 'PENDING' } : {}),
                }
              : {}),
          },
          include: professionalInclude,
        }),
      ),
    });
  }),
);
const availability = z
  .array(
    z
      .object({
        dayOfWeek: z.number().int().min(0).max(6),
        startMinute: z.number().int().min(480).max(1139),
        endMinute: z.number().int().min(481).max(1140),
      })
      .refine((a) => a.startMinute < a.endMinute),
  )
  .max(21)
  .refine(
    (rows) =>
      rows.every((a, i) =>
        rows.every(
          (b, j) =>
            i === j ||
            a.dayOfWeek !== b.dayOfWeek ||
            a.endMinute <= b.startMinute ||
            b.endMinute <= a.startMinute,
        ),
      ),
    'Availability windows overlap',
  );
router.put(
  '/professionals/availability',
  authenticate,
  authorize('PROFESSIONAL'),
  validate(z.object({ availability })),
  endpoint(async (req, res) => {
    const professionalId = req.user.professional.id;
    await db.$transaction(async (tx) => {
      await tx.$queryRaw`SELECT id FROM "Professional" WHERE id = ${professionalId} FOR UPDATE`;
      await tx.availability.deleteMany({ where: { professionalId } });
      await tx.availability.createMany({
        data: req.validated.availability.map((a) => ({ ...a, professionalId })),
      });
    });
    res.json({ availability: await db.availability.findMany({ where: { professionalId } }) });
  }),
);
router.get(
  '/professionals/earnings',
  authenticate,
  authorize('PROFESSIONAL'),
  endpoint(async (req, res) => {
    const paid = await db.payment.aggregate({
      where: {
        status: 'PAID',
        booking: { professionalId: req.user.professional.id, status: 'COMPLETED' },
      },
      _sum: { amount: true },
      _count: true,
    });
    res.json({ earnings: Number(paid._sum.amount || 0), paidBookings: paid._count });
  }),
);
router.get(
  '/professionals/:id',
  endpoint(async (req, res) => {
    const p = await db.professional.findFirst({
      where: { id: id.parse(req.params.id), verificationStatus: 'VERIFIED' },
      include: professionalInclude,
    });
    ensure(p, 404, 'Professional not found');
    res.json({ professional: professionalView(p) });
  }),
);
export default router;
