import 'dotenv/config';
import assert from 'node:assert/strict';
import { db } from '../src/config/db.js';
const before = {
  categories: await db.category.count(),
  services: await db.service.count(),
  professionals: await db.professional.count(),
  bookings: await db.booking.count(),
};
await db.$disconnect();
await db.$connect();
assert.equal(await db.booking.count(), before.bookings);
const invalid = await db.category.findMany({
  where: { isActive: true, services: { none: { isActive: true } } },
  select: { id: true },
});
assert.deepEqual(invalid, []);
console.log(JSON.stringify(before));
await db.$disconnect();
