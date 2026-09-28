import { Router } from 'express';
import { z } from 'zod';
import { db } from '../config/db.js';
import { endpoint, ensure } from '../utils/errors.js';
import { authenticate, authorize } from '../middleware/auth.js';
import { validate, text, id, pageQuery } from '../middleware/validate.js';
import { serviceInclude, serviceView } from '../utils/serializers.js';
import { listServices } from '../services/catalog.service.js';
const router = Router();
const query = pageQuery.extend({
  category: id.optional(),
  categoryId: id.optional(),
  search: z.string().trim().max(200).optional(),
  subcategory: text.optional(),
  location: text.optional(),
  city: text.optional(),
  sort: z.enum(['popular', 'rating', 'price-asc', 'price-desc']).default('popular'),
});
router.get(
  '/categories',
  endpoint(async (req, res) => {
    const rows = await db.category.findMany({
      where: { isActive: true },
      orderBy: { name: 'asc' },
      include: { _count: { select: { services: { where: { isActive: true } } } } },
    });
    res.json({
      categories: rows.map((c) => ({
        ...c,
        servicesCount: c._count.services,
        alt: c.name,
        subtitle: c.description,
      })),
    });
  }),
);
router.get(
  '/categories/:slug',
  endpoint(async (req, res) => {
    const category = await db.category.findFirst({
      where: { slug: req.params.slug, isActive: true },
    });
    ensure(category, 404, 'Category not found');
    res.json({ category });
  }),
);
router.get(
  '/services',
  validate(query, 'query'),
  endpoint(async (req, res) => res.json(await listServices(req.validated))),
);
router.get(
  '/search',
  endpoint(async (req, res) => {
    const data = await listServices(query.parse({ search: req.query.q }));
    res.json({ results: data.services, count: data.pagination.total });
  }),
);
router.get(
  ['/services/slug/:slug', '/services/:id'],
  endpoint(async (req, res) => {
    const key = id.parse(req.params.slug || req.params.id);
    const service = await db.service.findFirst({
      where: { isActive: true, category: { isActive: true }, OR: [{ id: key }, { slug: key }] },
      include: serviceInclude,
    });
    ensure(service, 404, 'Service not found');
    res.json({ service: serviceView(service) });
  }),
);
const serviceSchema = z.object({
  name: text,
  slug: id,
  categoryId: id,
  description: z.string().trim().min(10).max(5000),
  shortDesc: text,
  subcategory: text.optional(),
  priceType: z.enum(['FIXED', 'INSPECTION']).optional(),
  serviceType: z.enum(['HOME_VISIT', 'PICKUP_OR_HOME_VISIT']).optional(),
  warrantyPolicy: z.string().trim().max(500).nullable().optional(),
  requiredTools: z.array(text).max(30).optional(),
  image: z.url().max(2048),
  startingPrice: z.number().positive().max(1000000),
  duration: z.number().int().min(15).max(480),
  locations: z.array(text).min(1),
  isActive: z.boolean().optional(),
  whatIncluded: z.array(text).default([]),
  whatExcluded: z.array(text).default([]),
  whyChoose: z.array(text).default([]),
  steps: z.array(text).max(30).default([]),
});
router.post(
  '/services',
  authenticate,
  authorize('ADMIN'),
  validate(serviceSchema),
  endpoint(async (req, res) => {
    const d = req.validated;
    const service = await db.service.create({
      data: {
        ...d,
        variants: {
          create: {
            name: 'Standard package',
            price: d.startingPrice,
            durationMin: d.duration,
            description: d.shortDesc,
            included: d.whatIncluded,
          },
        },
      },
      include: serviceInclude,
    });
    res.status(201).json({ service: serviceView(service) });
  }),
);
router.put(
  '/services/:id',
  authenticate,
  authorize('ADMIN'),
  validate(serviceSchema.partial()),
  endpoint(async (req, res) => {
    const serviceId = id.parse(req.params.id);
    const service = await db.$transaction(async (tx) => {
      const variants = await tx.serviceVariant.findMany({ where: { serviceId } });
      if (req.validated.startingPrice && variants.length > 1)
        ensure(
          false,
          400,
          'Update package prices individually for services with multiple packages',
        );
      if (variants.length === 1)
        await tx.serviceVariant.update({
          where: { id: variants[0].id },
          data: {
            ...(req.validated.startingPrice ? { price: req.validated.startingPrice } : {}),
            ...(req.validated.duration ? { durationMin: req.validated.duration } : {}),
          },
        });
      return tx.service.update({
        where: { id: serviceId },
        data: req.validated,
        include: serviceInclude,
      });
    });
    res.json({ service: serviceView(service) });
  }),
);
router.put(
  '/services/:id/variants/:variantId',
  authenticate,
  authorize('ADMIN'),
  validate(
    z.object({
      name: text.optional(),
      price: z.number().positive().max(1000000).optional(),
      durationMin: z.number().int().min(15).max(480).optional(),
    }),
  ),
  endpoint(async (req, res) => {
    const serviceId = id.parse(req.params.id),
      variantId = id.parse(req.params.variantId);
    const service = await db.$transaction(async (tx) => {
      const result = await tx.serviceVariant.updateMany({
        where: { id: variantId, serviceId },
        data: req.validated,
      });
      ensure(result.count, 404, 'Package not found');
      const cheapest = await tx.serviceVariant.findFirstOrThrow({
        where: { serviceId },
        orderBy: { price: 'asc' },
      });
      return tx.service.update({
        where: { id: serviceId },
        data: { startingPrice: cheapest.price, duration: cheapest.durationMin },
        include: serviceInclude,
      });
    });
    res.json({ service: serviceView(service) });
  }),
);
router.delete(
  '/services/:id',
  authenticate,
  authorize('ADMIN'),
  endpoint(async (req, res) => {
    await db.service.update({ where: { id: id.parse(req.params.id) }, data: { isActive: false } });
    res.json({ success: true });
  }),
);
export default router;
