import 'dotenv/config';
import { test, after } from 'node:test';
import assert from 'node:assert/strict';
import { randomUUID } from 'node:crypto';
import bcrypt from 'bcrypt';
import { spawn } from 'node:child_process';
import { once } from 'node:events';
import request from 'supertest';

if (
  !process.env.TEST_DATABASE_URL ||
  !new URL(process.env.TEST_DATABASE_URL).pathname.endsWith('_test')
)
  throw new Error('Set TEST_DATABASE_URL to a separate database whose name ends in _test');
process.env.DATABASE_URL = process.env.TEST_DATABASE_URL;
process.env.NODE_ENV = 'test';
const { app } = await import('../src/app.js');
const { db } = await import('../src/config/db.js');
const origin = 'http://localhost:5173';
const password = `Test-${randomUUID()}`;
const customer = request.agent(app),
  other = request.agent(app),
  admin = request.agent(app),
  pro = request.agent(app);
const send = (agent, method, url, body = {}) => agent[method](url).set('Origin', origin).send(body);
let customerId, professionalId, addressId, service, booking, secondBooking;
const date = new Date(Date.now() + 3 * 86400000).toISOString().slice(0, 10);
const payload = () => ({
  addressId,
  items: [{ serviceId: service.id, variantId: service.variants[0].id, quantity: 1 }],
  bookingDate: date,
  bookingTime: '10:00 AM - 11:00 AM',
  paymentMethod: 'CASH',
  total: 1,
  subtotal: 1,
  customerId: 'forged',
});
const book = (agent = customer, body = payload(), key = randomUUID()) =>
  send(agent, 'post', '/api/bookings', body).set('Idempotency-Key', key);
after(async () => {
  await db.$disconnect();
});

test('PostgreSQL-backed marketplace integration', async (t) => {
  // Only the dedicated _test database is reset. Never point this at user data.
  await db.$executeRawUnsafe('TRUNCATE "User", "Category", "Offer" RESTART IDENTITY CASCADE');
  process.env.SEED_DEMO = 'true';
  process.env.DEMO_PASSWORD = password;
  await import('../prisma/seed.js');
  await t.test(
    'health and real registration with hashed password and HttpOnly cookie',
    async () => {
      await request(app).get('/api/health').expect(200);
      const response = await send(customer, 'post', '/api/auth/register', {
        name: 'Test Customer',
        email: 'customer@example.test',
        password,
      }).expect(201);
      customerId = response.body.user.id;
      assert.ok(response.headers['set-cookie'][0].includes('HttpOnly'));
      assert.equal(response.body.user.passwordHash, undefined);
      const stored = await db.user.findUnique({ where: { id: customerId } });
      assert.notEqual(stored.passwordHash, password);
      assert.ok(await bcrypt.compare(password, stored.passwordHash));
      await send(other, 'post', '/api/auth/register', {
        name: 'Other Customer',
        email: 'other@example.test',
        password,
      }).expect(201);
    },
  );
  await t.test('duplicate, weak password and admin self-registration are rejected', async () => {
    await send(request(app), 'post', '/api/auth/register', {
      name: 'Duplicate',
      email: 'CUSTOMER@example.test',
      password,
    }).expect(409);
    await send(request(app), 'post', '/api/auth/register', {
      name: 'Weak',
      email: 'weak@example.test',
      password: 'short',
    }).expect(400);
    await send(request(app), 'post', '/api/auth/register', {
      name: 'Escalation',
      email: 'admin2@example.test',
      password,
      role: 'ADMIN',
    }).expect(400);
  });
  await t.test('real login, me, profile, invalid tokens and wrong password', async () => {
    await send(request(app), 'post', '/api/auth/login', {
      email: 'customer@example.test',
      password: 'incorrect',
    }).expect(401);
    await send(customer, 'post', '/api/auth/login', {
      email: 'customer@example.test',
      password,
    }).expect(200);
    await customer.get('/api/auth/me').expect(200);
    await request(app).get('/api/auth/me').set('Authorization', 'Bearer forged').expect(401);
    const r = await send(customer, 'put', '/api/auth/profile', {
      name: 'Updated Customer',
      role: 'ADMIN',
    }).expect(200);
    assert.equal(r.body.user.role, 'CUSTOMER');
  });
  await t.test('role restrictions and CSRF protection', async () => {
    await customer.get('/api/admin/metrics').expect(403);
    await customer.get('/api/professionals/bookings').expect(403);
    await request(app).get('/api/bookings').expect(401);
    await customer
      .put('/api/auth/profile')
      .set('Origin', 'https://evil.test')
      .send({ name: 'Bad' })
      .expect(403);
    await customer.put('/api/auth/profile').send({ name: 'Bad' }).expect(403);
  });
  await t.test(
    'all seeded categories have valid services; Sofa Cleaning relationship is correct',
    async () => {
      const categories = (await request(app).get('/api/categories').expect(200)).body.categories;
      assert.ok(categories.length >= 14);
      for (const c of categories) {
        assert.ok(c.servicesCount > 0, c.name);
        const data = (await request(app).get(`/api/services?category=${c.id}`).expect(200)).body;
        assert.equal(data.pagination.total, c.servicesCount);
        for (const s of data.services) {
          assert.equal(s.categoryId, c.id);
          assert.ok(s.name && s.description && s.image && s.startingPrice > 0 && s.variants.length);
        }
        await request(app).get(`/api/categories/${c.slug}`).expect(200);
      }
      const sofa = (await request(app).get('/api/services?category=sofa-cleaning').expect(200)).body
        .services;
      assert.ok(sofa.some((s) => s.id === 'srv-sofa-cleaning'));
      service = (await request(app).get('/api/services/srv-ac-foamjet').expect(200)).body.service;
      assert.ok(Array.isArray(service.steps));
      await request(app).get(`/api/services/slug/${service.slug}`).expect(200);
    },
  );
  await t.test(
    'search, location, sort, pagination, empty results and invalid queries',
    async () => {
      assert.ok((await request(app).get('/api/services?search=sofa')).body.services.length > 0);
      assert.equal(
        (await request(app).get('/api/services?location=Unknown')).body.services.length,
        0,
      );
      const sorted = (await request(app).get('/api/services?sort=price-asc&limit=100')).body
        .services;
      assert.deepEqual(
        sorted.map((s) => s.startingPrice),
        sorted.map((s) => s.startingPrice).sort((a, b) => a - b),
      );
      assert.equal(
        (await request(app).get('/api/services?limit=1&page=2')).body.services.length,
        1,
      );
      await request(app).get('/api/services?page=-1').expect(400);
      await request(app).get('/api/services?sort=bad').expect(400);
      await request(app).get('/api/services/unknown').expect(404);
      await request(app).get('/api/search?q=sofa').expect(200);
    },
  );
  await t.test('expanded catalog has unique services, working filters and explicit inspection fees', async () => {
    const rows = await db.service.findMany({ where: { isDemo: true }, select: {
      name: true, slug: true, categoryId: true, subcategory: true, priceType: true,
      image: true, variants: { select: { price: true } },
    } });
    assert.ok(rows.length >= 138);
    assert.equal(new Set(rows.map((row) => row.slug)).size, rows.length);
    assert.equal(new Set(rows.map((row) => `${row.categoryId}:${row.name.toLowerCase()}`)).size, rows.length);
    const categories = new Set(rows.map((row) => row.categoryId));
    for (const category of ['cat-beauty', 'cat-laptop-computer', 'cat-electronics', 'cat-electrician', 'cat-plumber', 'cat-cleaning', 'cat-ac-appliances']) assert.ok(categories.has(category));
    assert.ok(rows.every((row) => row.image && row.variants.length > 0));
    const filtered = (await request(app).get('/api/services?category=cat-beauty&subcategory=Manicure%20%26%20Pedicure&limit=100').expect(200)).body;
    assert.ok(filtered.services.length >= 9);
    assert.ok(filtered.services.every((s) => s.subcategory === 'Manicure & Pedicure'));
    const search = (await request(app).get('/api/services?search=%20%20laptop%20%20screen%20%20replacement%20&limit=100').expect(200)).body;
    assert.ok(search.services.some((s) => s.name === 'Laptop Screen Replacement' && s.priceType === 'INSPECTION'));
    for (const [id, category, subcategory] of [
      ['pro-demo-nails', 'cat-beauty', 'Manicure & Pedicure'],
      ['pro-demo-computers', 'cat-laptop-computer', null],
      ['pro-demo-electronics', 'cat-electronics', null],
      ['pro-demo-appliances', 'cat-ac-appliances', 'Appliances'],
    ]) {
      const professional = await db.professional.findUnique({ where: { id }, select: { services: { select: { categoryId: true, subcategory: true } } } });
      assert.ok(professional?.services.length, id);
      assert.ok(professional.services.every((service) =>
        service.categoryId === category &&
        (!subcategory || service.subcategory === subcategory)), id);
    }
  });
  await t.test('RO service keeps its price and serves local gallery images', async () => {
    const { service: ro } = (await request(app).get('/api/services/slug/ro-water-purifier-service').expect(200)).body;
    assert.equal(ro.id, 'srv-water-purifier-ro');
    assert.equal(ro.name, 'RO Water Purifier Service & Filter Replacement');
    assert.equal(ro.categoryName, 'Water Purifier');
    assert.equal(ro.startingPrice, 399);
    assert.equal(ro.durationMin, 40);
    assert.equal(ro.isActive, true);
    assert.equal(ro.image, '/service-images/ro-purifier/servicing.png');
    assert.equal(ro.galleryImages.length, 4);
    assert.equal(new Set([ro.image, ...ro.galleryImages]).size, 5);
    assert.equal(await db.service.count({ where: { id: ro.id } }), 1);
  });
  await t.test('address persistence, ownership and default address', async () => {
    const a = await send(customer, 'post', '/api/addresses', {
      house: '10',
      street: 'Test Road',
      city: 'Agra',
      state: 'Uttar Pradesh',
      pincode: '282001',
      isDefault: true,
    }).expect(201);
    addressId = a.body.address.id;
    await send(customer, 'put', `/api/addresses/${addressId}/default`).expect(200);
    await send(other, 'put', `/api/addresses/${addressId}/default`).expect(404);
    assert.equal((await customer.get('/api/addresses')).body.addresses.length, 1);
  });
  await t.test(
    'booking prices are server-calculated; payment stays pending; retries deduplicate',
    async () => {
      const key = randomUUID();
      const first = await book(customer, payload(), key).expect(201);
      booking = first.body.booking;
      assert.equal(booking.total, service.variants[0].price);
      assert.equal(booking.paymentStatus, 'PENDING');
      assert.equal(booking.customerId, customerId);
      assert.equal(booking.status, 'PENDING');
      const repeat = await book(customer, payload(), key).expect(201);
      assert.equal(repeat.body.booking.id, booking.id);
      assert.equal(await db.booking.count({ where: { customerId } }), 1);
    },
  );
  await t.test(
    'invalid address, package, quantity, date and unsupported payment rejected',
    async () => {
      await book(other).expect(400);
      await book(customer, { ...payload(), bookingDate: '2020-01-01' }).expect(400);
      await book(customer, { ...payload(), bookingTime: '10:00 AM - 09:00 AM' }).expect(400);
      const longVariant = service.variants.find((v) => v.durationMin === 80);
      assert.ok(longVariant);
      await book(customer, {
        ...payload(),
        bookingTime: '06:00 PM - 07:00 PM',
        items: [{ serviceId: service.id, variantId: longVariant.id, quantity: 6 }],
      }).expect(400);
      await book(customer, {
        ...payload(),
        items: [{ serviceId: service.id, variantId: 'wrong', quantity: 1 }],
      }).expect(400);
      await book(customer, {
        ...payload(),
        items: [{ serviceId: service.id, quantity: -1 }],
      }).expect(400);
      await book(customer, { ...payload(), paymentMethod: 'CARD' }).expect(400);
      await send(customer, 'post', '/api/bookings', payload()).expect(400);
    },
  );
  await t.test('new catalog service books with the database inspection fee', async () => {
    const laptop = await db.service.findFirstOrThrow({
      where: { name: 'Laptop Screen Replacement', categoryId: 'cat-laptop-computer' },
      include: { variants: true },
    });
    const response = await book(customer, {
      addressId,
      items: [{ serviceId: laptop.id, variantId: laptop.variants[0].id, quantity: 1 }],
      bookingDate: new Date(Date.now() + 4 * 86400000).toISOString().slice(0, 10),
      bookingTime: '10:00 AM - 11:00 AM',
      paymentMethod: 'CASH',
      total: 1,
    }).expect(201);
    assert.equal(response.body.booking.total, Number(laptop.variants[0].price));
    assert.equal(response.body.booking.status, 'PENDING');
    assert.ok(await db.booking.findUnique({ where: { id: response.body.booking.id } }));
  });
  await t.test('booking ownership and status transition enforcement', async () => {
    assert.equal(
      (await other.get(`/api/bookings?customerId=${customerId}`)).body.bookings.length,
      0,
    );
    await other.get(`/api/bookings/${booking.id}`).expect(404);
    await send(other, 'put', `/api/bookings/${booking.id}/cancel`).expect(404);
    await send(customer, 'put', `/api/bookings/${booking.id}/status`, {
      status: 'COMPLETED',
    }).expect(409);
    await customer.get(`/api/bookings/${booking.id}`).expect(200);
  });
  await t.test('professional registration, profile, services and availability', async () => {
    await send(pro, 'post', '/api/professionals/register', {
      name: 'Test Pro',
      email: 'pro@example.test',
      password,
    }).expect(201);
    let p = (await pro.get('/api/professionals/profile').expect(200)).body.professional;
    professionalId = p.id;
    assert.equal(p.verificationStatus, 'PENDING');
    await send(pro, 'put', '/api/professionals/profile', {
      businessName: 'Test Repairs',
      serviceIds: [service.id],
      serviceArea: ['Agra'],
      isAvailableToday: true,
    }).expect(200);
    await send(pro, 'put', '/api/professionals/availability', {
      availability: Array.from({ length: 7 }, (_, dayOfWeek) => ({
        dayOfWeek,
        startMinute: 480,
        endMinute: 1140,
      })),
    }).expect(200);
    await send(pro, 'put', '/api/professionals/availability', {
      availability: [{ dayOfWeek: 0, startMinute: 900, endMinute: 500 }],
    }).expect(400);
    await pro.get('/api/admin/metrics').expect(403);
  });
  await t.test(
    'admin authentication, metrics, category/service CRUD and professional verification',
    async () => {
      await db.user.create({
        data: {
          name: 'Test Admin',
          email: 'admin@example.test',
          passwordHash: await bcrypt.hash(password, 12),
          role: 'ADMIN',
        },
      });
      await send(admin, 'post', '/api/auth/login', {
        email: 'admin@example.test',
        password,
      }).expect(200);
      await admin.get('/api/admin/metrics').expect(200);
      await admin.get('/api/admin/services').expect(200);
      await admin.get('/api/admin/users').expect(200);
      await admin.get('/api/admin/professionals').expect(200);
      await admin.get('/api/admin/categories').expect(200);
      const c = (
        await send(admin, 'post', '/api/admin/categories', {
          name: 'Test Category',
          slug: 'test-category',
          description: 'Testing category',
          image: 'https://example.test/image.jpg',
        }).expect(201)
      ).body.category;
      await send(admin, 'put', `/api/admin/categories/${c.id}`, {
        description: 'Updated category',
      }).expect(200);
      const s = (
        await send(admin, 'post', '/api/services', {
          categoryId: c.id,
          name: 'Test Service',
          slug: 'test-service',
          description: 'A test service description',
          shortDesc: 'Test visit',
          image: 'https://example.test/image.jpg',
          startingPrice: 100,
          duration: 30,
          locations: ['Agra'],
        }).expect(201)
      ).body.service;
      const updated = (
        await send(admin, 'put', `/api/services/${s.id}`, { startingPrice: 200 }).expect(200)
      ).body.service;
      assert.equal(updated.variants[0].price, 200);
      await send(admin, 'put', `/api/services/${s.id}/variants/${s.variants[0].id}`, {
        price: 250,
      }).expect(200);
      await send(admin, 'delete', `/api/services/${s.id}`).expect(200);
      await request(app).get(`/api/services/${s.id}`).expect(404);
      await send(admin, 'delete', `/api/admin/categories/${c.id}`).expect(200);
      await send(admin, 'put', `/api/admin/professionals/${professionalId}/verification`, {
        verificationStatus: 'VERIFIED',
      }).expect(200);
      await request(app).get(`/api/professionals/${professionalId}`).expect(200);
    },
  );
  await t.test('assignment checks availability and concurrent overlaps', async () => {
    secondBooking = (await book().expect(201)).body.booking;
    await customer.get(`/api/admin/bookings/${booking.id}/eligible-professionals`).expect(403);
    const candidates = (await admin.get(`/api/admin/bookings/${booking.id}/eligible-professionals`).expect(200)).body.professionals;
    assert.ok(candidates.some((p) => p.id === professionalId));
    const results = await Promise.all(
      [booking, secondBooking].map((b) =>
        send(admin, 'put', `/api/bookings/${b.id}/status`, { status: 'ASSIGNED', professionalId }),
      ),
    );
    assert.deepEqual(results.map((r) => r.status).sort(), [200, 409]);
    if (results[1].status === 200) [booking, secondBooking] = [secondBooking, booking];
    const jobs = (await pro.get('/api/professionals/bookings')).body.bookings;
    assert.equal(jobs.length, 1);
    assert.equal(jobs[0].verificationOtp, undefined);
    const requests = (await pro.get('/api/professionals/booking-requests').expect(200)).body.bookings;
    assert.equal(requests.length, 1);
    assert.equal(requests[0].id, booking.id);
    const professionalNotes = (await pro.get('/api/notifications').expect(200)).body.notifications;
    assert.ok(professionalNotes.some((n) => n.title === 'Professional assigned'));
  });
  await t.test('professional rejects and accepts assigned bookings', async () => {
    await send(other, 'post', `/api/bookings/${booking.id}/accept`).expect(403);
    await send(pro, 'put', '/api/professionals/profile', { isAvailableToday: false }).expect(200);
    await send(pro, 'post', `/api/bookings/${booking.id}/accept`).expect(409);
    await send(pro, 'put', '/api/professionals/profile', { isAvailableToday: true }).expect(200);
    await send(pro, 'post', `/api/bookings/${booking.id}/reject`).expect(200);
    assert.equal((await pro.get('/api/professionals/rejected-bookings').expect(200)).body.requests[0].bookingId, booking.id);
    assert.equal((await db.bookingAssignment.findFirst({ where: { bookingId: booking.id, rejectedAt: { not: null } } })).activeKey, null);
    await send(admin, 'put', `/api/bookings/${booking.id}/status`, {
      status: 'ASSIGNED',
      professionalId,
    }).expect(200);
    const simultaneous = await Promise.all([
      send(pro, 'post', `/api/bookings/${booking.id}/accept`),
      send(pro, 'post', `/api/bookings/${booking.id}/accept`),
    ]);
    assert.deepEqual(simultaneous.map((r) => r.status).sort(), [200, 409]);
    await send(pro, 'post', `/api/bookings/${booking.id}/accept`).expect(409);
    assert.ok((await db.bookingAssignment.findFirst({ where: { activeKey: booking.id } })).acceptedAt);
    await send(pro, 'put', `/api/bookings/${booking.id}/status`, { status: 'ON_THE_WAY' }).expect(
      200,
    );
    await send(customer, 'put', `/api/bookings/${booking.id}/cancel`).expect(403);
    await send(pro, 'put', `/api/bookings/${booking.id}/status`, { status: 'ARRIVED' }).expect(200);
  });
  await t.test('OTP verified on server, completion does not mark payment paid', async () => {
    const wrong = booking.verificationOtp === '0000' ? '1111' : '0000';
    await send(pro, 'put', `/api/bookings/${booking.id}/status`, {
      status: 'IN_PROGRESS',
      otp: wrong,
    }).expect(400);
    assert.equal((await db.booking.findUnique({ where: { id: booking.id } })).otpAttempts, 1);
    await send(pro, 'put', `/api/bookings/${booking.id}/status`, {
      status: 'IN_PROGRESS',
      otp: booking.verificationOtp,
    }).expect(200);
    const r = await send(pro, 'put', `/api/bookings/${booking.id}/status`, {
      status: 'COMPLETED',
    }).expect(200);
    assert.equal(r.body.booking.paymentStatus, 'PENDING');
    assert.equal((await pro.get('/api/professionals/earnings')).body.earnings, 0);
  });
  await t.test('admin cash reconciliation and real earnings', async () => {
    await send(customer, 'put', `/api/admin/bookings/${booking.id}/payment`, {
      transactionId: 'receipt-1',
    }).expect(403);
    await send(admin, 'put', `/api/admin/bookings/${booking.id}/payment`, {
      transactionId: 'receipt-1',
    }).expect(200);
    await send(admin, 'put', `/api/admin/bookings/${booking.id}/payment`, {
      transactionId: 'receipt-2',
    }).expect(409);
    assert.equal((await pro.get('/api/professionals/earnings')).body.earnings, booking.total);
  });
  await t.test('completed booking reviews, ownership and duplicates', async () => {
    await send(other, 'post', '/api/reviews', {
      bookingId: booking.id,
      rating: 5,
      comment: 'Not my booking',
    }).expect(400);
    await send(customer, 'post', '/api/reviews', {
      bookingId: booking.id,
      rating: 5,
      comment: 'Great service',
    }).expect(201);
    await send(customer, 'post', '/api/reviews', {
      bookingId: booking.id,
      rating: 5,
      comment: 'Duplicate',
    }).expect(409);
    assert.equal(
      (await request(app).get(`/api/reviews?serviceId=${service.id}`)).body.reviews.length,
      1,
    );
    assert.equal((await request(app).get(`/api/services/${service.id}`)).body.service.rating, 5);
  });
  await t.test('cancellation persists and terminal bookings cannot change', async () => {
    await send(customer, 'put', `/api/bookings/${secondBooking.id}/cancel`).expect(200);
    await send(admin, 'put', `/api/bookings/${booking.id}/status`, { status: 'CANCELLED' }).expect(
      409,
    );
  });
  await t.test('coupons and discounted totals', async () => {
    await db.offer.create({
      data: {
        code: 'TEST50',
        description: 'Test offer',
        discountType: 'FLAT',
        value: 50,
        minBookingAmount: 100,
        expiry: new Date(Date.now() + 86400000),
      },
    });
    await request(app).get('/api/coupons').expect(200);
    await request(app).get('/api/offers').expect(200);
    const cart = [{ serviceId: service.id, variantId: service.variants[0].id, quantity: 1 }];
    const preview = await send(customer, 'post', '/api/coupons/validate', {
      code: 'TEST50',
      items: cart,
    }).expect(200);
    assert.equal(preview.body.discount, 50);
    assert.equal(preview.body.subtotal, service.variants[0].price);
    await send(customer, 'post', '/api/coupons/validate', { code: 'invalid', items: cart }).expect(
      400,
    );
    const r = await book(customer, { ...payload(), couponCode: 'TEST50' }).expect(201);
    assert.equal(r.body.booking.total, service.variants[0].price - 50);
  });
  await t.test('notifications and support tickets enforce ownership', async () => {
    const notifications = (await customer.get('/api/notifications')).body.notifications;
    assert.ok(notifications.length);
    await send(other, 'patch', `/api/notifications/${notifications[0].id}/read`).expect(404);
    await send(customer, 'patch', `/api/notifications/${notifications[0].id}/read`).expect(200);
    const ticket = (
      await send(customer, 'post', '/api/support/tickets', {
        subject: 'Help with booking',
        message: 'Please assist with my booking.',
      }).expect(201)
    ).body.ticket;
    assert.equal((await other.get('/api/support/tickets')).body.tickets.length, 0);
    await send(admin, 'put', `/api/admin/support/tickets/${ticket.id}`, {
      status: 'RESOLVED',
    }).expect(200);
  });
  await t.test('database constraints prevent invalid payments and item relations', async () => {
    await assert.rejects(
      db.payment.update({ where: { bookingId: booking.id }, data: { amount: 1 } }),
    );
    await assert.rejects(
      db.review.update({ where: { bookingId: booking.id }, data: { rating: 7 } }),
    );
    const item = await db.bookingItem.findFirst({ where: { bookingId: booking.id } });
    await assert.rejects(
      db.bookingItem.update({ where: { id: item.id }, data: { serviceId: 'srv-sofa-cleaning' } }),
    );
  });
  await t.test('password change revokes sessions; logout invalidates JWT session', async () => {
    await send(customer, 'post', '/api/auth/change-password', {
      currentPassword: password,
      password: `${password}!`,
    }).expect(200);
    await customer.get('/api/auth/me').expect(401);
    await send(customer, 'post', '/api/auth/login', {
      email: 'customer@example.test',
      password: `${password}!`,
    }).expect(200);
    await send(customer, 'post', '/api/auth/logout').expect(200);
    await customer.get('/api/auth/me').expect(401);
  });
  await t.test(
    'AI has an honest unconfigured state and data survives new DB connection',
    async () => {
      delete process.env.GEMINI_API_KEY;
      await send(request(app), 'post', '/api/ai/chat', { message: 'Find cleaning' }).expect(503);
      const count = await db.booking.count();
      await db.$disconnect();
      await db.$connect();
      assert.equal(await db.booking.count(), count);
    },
  );
  await t.test('sessions and bookings survive an API process restart', async () => {
    const port = 5099;
    const base = `http://127.0.0.1:${port}/api`;
    let child;
    const start = async () => {
      child = spawn(process.execPath, ['src/server.js'], {
        env: { ...process.env, PORT: String(port) },
        windowsHide: true,
        stdio: 'ignore',
      });
      for (let attempt = 0; attempt < 300; attempt++) {
        assert.equal(child.exitCode, null, 'API exited during startup');
        try {
          if ((await fetch(`${base}/health`, { signal: AbortSignal.timeout(500) })).ok) return;
        } catch {}
        await new Promise((resolve) => setTimeout(resolve, 100));
      }
      throw new Error('API did not become ready');
    };
    const stop = async () => {
      if (child && child.exitCode === null) {
        const exited = once(child, 'exit');
        child.kill('SIGTERM');
        await exited;
      }
    };
    try {
      await start();
      const login = await fetch(`${base}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Origin: origin },
        body: JSON.stringify({ email: 'customer@example.test', password: `${password}!` }),
      });
      assert.equal(login.status, 200);
      const cookie = login.headers.getSetCookie()[0].split(';')[0];
      const first = await fetch(`${base}/bookings/${booking.id}`, { headers: { Cookie: cookie } });
      assert.equal(first.status, 200);
      await stop();
      await start();
      const persisted = await fetch(`${base}/bookings/${booking.id}`, {
        headers: { Cookie: cookie },
      });
      assert.equal(persisted.status, 200);
      assert.equal((await persisted.json()).booking.id, booking.id);
    } finally {
      await stop();
    }
  });
});
