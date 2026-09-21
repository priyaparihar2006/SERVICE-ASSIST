import 'dotenv/config';
import bcrypt from 'bcrypt';
import { db } from '../src/config/db.js';
import { CATEGORIES, SERVICES, PROFESSIONALS, CITIES, COUPONS } from './catalog.js';
import { password } from '../src/middleware/validate.js';
if (process.env.NODE_ENV === 'production' || process.env.SEED_DEMO !== 'true')
  throw new Error('Demo seeding requires SEED_DEMO=true outside production');
const demoPassword = password.parse(process.env.DEMO_PASSWORD);
try {
  await db.$transaction(
    async (tx) => {
      for (const c of CATEGORIES) {
        const { servicesCount, ...data } = c;
        await tx.category.upsert({ where: { id: c.id }, create: data, update: {} });
      }
      const corrections = {
        'srv-sofa-cleaning': 'cat-sofa-cleaning',
        'srv-bathroom-deep': 'cat-bathroom-cleaning',
        'srv-pest-control': 'cat-pest-control',
      };
      const catalog = SERVICES.map((s) => ({
        ...s,
        categoryId: corrections[s.id] || s.categoryId,
      }));
      const additions = {
        'cat-cleaning': ['Whole-home deep cleaning', 2499, 180],
        'cat-carpenter': ['Furniture assembly and hinge repair', 399, 60],
        'cat-painting': ['Interior wall painting consultation', 499, 60],
      };
      for (const c of CATEGORIES.filter((c) => !catalog.some((s) => s.categoryId === c.id))) {
        const [name, price, duration] = additions[c.id] || [`${c.name} consultation`, 499, 60];
        catalog.push({
          id: `srv-demo-${c.id}`,
          slug: `${c.slug}-consultation`,
          name,
          categoryId: c.id,
          description: `${name}. A professional inspects your requirements and provides an itemized quote before additional work.`,
          shortDesc: c.description,
          image: c.image,
          startingPrice: price,
          durationMin: duration,
          whatIncluded: ['Inspection and consultation', 'Itemized estimate'],
          whatExcluded: ['Additional parts and work'],
          whyChoose: ['Upfront package pricing'],
          faqs: [],
          variants: [
            {
              id: `var-demo-${c.id}`,
              name: 'Standard visit',
              price,
              durationMin: duration,
              description: 'Inspection and consultation',
              included: ['Professional visit'],
            },
          ],
        });
      }
      for (const s of catalog) {
        const data = {
          id: s.id,
          name: s.name,
          slug: s.slug,
          categoryId: s.categoryId,
          description: s.description,
          shortDesc: s.shortDesc,
          image: s.image,
          startingPrice: s.startingPrice,
          duration: s.durationMin,
          rating: 0,
          popular: s.popular || false,
          trending: s.trending || false,
          locations: CITIES.map((c) => c.name),
          whatIncluded: s.whatIncluded,
          whatExcluded: s.whatExcluded,
          whyChoose: s.whyChoose,
          faqs: s.faqs,
        };
        await tx.service.upsert({ where: { id: s.id }, create: data, update: {} });
        for (const v of s.variants)
          await tx.serviceVariant.upsert({
            where: { id: v.id },
            create: { ...v, serviceId: s.id },
            update: {},
          });
      }
      const passwordHash = await bcrypt.hash(demoPassword, 12);
      for (const [i, p] of PROFESSIONALS.entries()) {
        const email = `professional${i + 1}@demo.service-assist.test`;
        const user = await tx.user.upsert({
          where: { email },
          update: {},
          create: {
            name: `${p.name} (Demo)`,
            email,
            passwordHash,
            role: 'PROFESSIONAL',
            isDemo: true,
            profileImage: p.avatar,
          },
        });
        if (!user.isDemo) throw new Error('Refusing to modify non-demo user');
        const serviceIds = catalog
          .filter(
            (s) =>
              s.categoryId === p.categoryId ||
              (p.categoryId === 'cat-cleaning' &&
                ['cat-sofa-cleaning', 'cat-bathroom-cleaning', 'cat-pest-control'].includes(
                  s.categoryId,
                )),
          )
          .map((s) => ({ id: s.id }));
        await tx.professional.upsert({
          where: { userId: user.id },
          update: {},
          create: {
            userId: user.id,
            id: p.id,
            businessName: p.profession,
            description: p.bio,
            experience: p.experienceYears,
            verificationStatus: 'VERIFIED',
            serviceArea: CITIES.map((c) => c.name),
            isAvailableToday: true,
            services: { connect: serviceIds },
            availability: {
              create: Array.from({ length: 7 }, (_, dayOfWeek) => ({
                dayOfWeek,
                startMinute: 480,
                endMinute: 1140,
              })),
            },
          },
        });
      }
      for (const offer of COUPONS) {
        const { categoryLimit, ...data } = offer;
        if (!categoryLimit)
          await tx.offer.upsert({
            where: { code: offer.code },
            create: { ...data, expiry: new Date(`${offer.expiry}T23:59:59Z`) },
            update: {},
          });
      }
    },
    { timeout: 60000 },
  );
  console.log(
    'Demo catalog seeded without overwriting existing records. Passwords come from DEMO_PASSWORD.',
  );
} finally {
  await db.$disconnect();
}
