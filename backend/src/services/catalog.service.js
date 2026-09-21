import { db } from '../config/db.js';
import { serviceInclude, serviceView } from '../utils/serializers.js';
export async function listServices(q) {
  const category = q.category || q.categoryId;
  const location = q.location || q.city;
  const where = {
    isActive: true,
    category: { isActive: true },
    ...(category
      ? {
          OR: [
            { categoryId: category === 'cat-home-cleaning' ? 'cat-cleaning' : category },
            { category: { slug: category, isActive: true } },
          ],
        }
      : {}),
    ...(location ? { locations: { has: location } } : {}),
    ...(q.search
      ? {
          AND: [
            {
              OR: ['name', 'description', 'shortDesc']
                .map((k) => ({ [k]: { contains: q.search, mode: 'insensitive' } }))
                .concat([{ category: { name: { contains: q.search, mode: 'insensitive' } } }]),
            },
          ],
        }
      : {}),
  };
  const orderBy =
    q.sort === 'price-asc'
      ? { startingPrice: 'asc' }
      : q.sort === 'price-desc'
        ? { startingPrice: 'desc' }
        : q.sort === 'rating'
          ? { rating: 'desc' }
          : { createdAt: 'desc' };
  const [rows, total] = await db.$transaction([
    db.service.findMany({
      where,
      orderBy: [orderBy, { id: 'asc' }],
      skip: (q.page - 1) * q.limit,
      take: q.limit,
      include: serviceInclude,
    }),
    db.service.count({ where }),
  ]);
  return {
    services: rows.map(serviceView),
    pagination: { page: q.page, limit: q.limit, total, pages: Math.ceil(total / q.limit) },
  };
}
