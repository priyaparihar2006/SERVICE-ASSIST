import 'dotenv/config';
import { test, after } from 'node:test';
import assert from 'node:assert/strict';
import { randomUUID } from 'node:crypto';
import { execFileSync } from 'node:child_process';
import request from 'supertest';

if (
  !process.env.TEST_DATABASE_URL ||
  !new URL(process.env.TEST_DATABASE_URL).pathname.endsWith('_test')
)
  throw new Error('Set TEST_DATABASE_URL to a separate database whose name ends in _test');
process.env.DATABASE_URL = process.env.TEST_DATABASE_URL;
process.env.NODE_ENV = 'test';
process.env.FRONTEND_ORIGINS = 'http://localhost:5173,http://127.0.0.1:5173';

const { app } = await import('../src/app.js');
const { db } = await import('../src/config/db.js');
const origin = 'http://localhost:5173';
const password = `Test-${randomUUID()}`;
const send = (agent, method, url, body = {}, o = origin) =>
  agent[method](url).set('Origin', o).send(body);
const uuid = () => randomUUID();
after(async () => {
  await db.$disconnect();
});

// Every validate call counts against a 30/minute limit, so keep the calls before the limiter test few.
test('coupons: trusted origins, server-side pricing, restrictions and abuse resistance', async (t) => {
  await db.$executeRawUnsafe('TRUNCATE "User", "Category", "Offer" RESTART IDENTITY CASCADE');
  process.env.SEED_DEMO = 'true';
  process.env.DEMO_PASSWORD = password;
  await import('../prisma/seed.js');

  const first = (await request(app).get('/api/services?category=cat-cleaning&limit=1')).body
    .services[0];
  const lighting = (
    await request(app).get('/api/services?search=lighting&limit=20')
  ).body.services.find((s) => s.categoryId === 'cat-home-improvement');
  const ac = (await request(app).get('/api/services/srv-ac-foamjet')).body.service;
  assert.ok(first && lighting && ac, 'seed provides cleaning, lighting and AC services');
  const line = (service, target, variant = service.variants[0]) => ({
    serviceId: service.id,
    variantId: variant.id,
    quantity: Math.min(10, Math.max(1, Math.ceil(target / variant.price))),
  });
  const price = (l, service) =>
    service.variants.find((v) => v.id === l.variantId).price * l.quantity;
  const validate = (agent, code, items, extra = {}, o = origin) =>
    send(agent, 'post', '/api/coupons/validate', { code, items, ...extra }, o);

  const anon = request.agent(app);
  const customer = request.agent(app);
  const other = request.agent(app);
  await send(customer, 'post', '/api/auth/register', {
    name: 'Coupon Customer',
    email: 'c1@example.test',
    password,
  }).expect(201);
  await send(other, 'post', '/api/auth/register', {
    name: 'Other Customer',
    email: 'c2@example.test',
    password,
  }).expect(201);
  const addressId = (
    await send(customer, 'post', '/api/addresses', {
      house: '1',
      street: 'Test',
      city: 'Agra',
      state: 'Uttar Pradesh',
      pincode: '282001',
      isDefault: true,
    }).expect(201)
  ).body.address.id;
  const otherAddressId = (
    await send(other, 'post', '/api/addresses', {
      house: '2',
      street: 'Test',
      city: 'Agra',
      state: 'Uttar Pradesh',
      pincode: '282001',
      isDefault: true,
    }).expect(201)
  ).body.address.id;
  const date = new Date(Date.now() + 3 * 86400000).toISOString().slice(0, 10);
  const book = (agent, addr, items, body = {}, slot = '10:00 AM - 11:00 AM') =>
    send(agent, 'post', '/api/bookings', {
      addressId: addr,
      items,
      bookingDate: date,
      bookingTime: slot,
      paymentMethod: 'CASH',
      ...body,
    }).set('Idempotency-Key', uuid());

  await t.test('origin policy: exact allow-list, no wildcard, CSRF rule intact', async () => {
    const cart = [line(ac, 600)];
    // Both names of the dev server are trusted; anything else is refused with the reported error.
    await validate(anon, 'WELCOME150', cart, {}, 'http://localhost:5173').expect(200);
    await validate(anon, 'WELCOME150', cart, {}, 'http://127.0.0.1:5173').expect(200);
    for (const bad of [
      'http://localhost:5174',
      'http://127.0.0.1:5175',
      'https://evil.example',
      'http://localhost:5173.evil.example',
      'http://localhost',
      'null',
    ]) {
      const res = await validate(anon, 'WELCOME150', cart, {}, bad).expect(403);
      assert.equal(res.body.error, 'Origin is not allowed', bad);
    }
    // A cookie-authenticated request must carry an Origin, and JSON is required.
    await customer
      .post('/api/coupons/validate')
      .send({ code: 'WELCOME150', items: cart })
      .expect(403);
    await customer
      .post('/api/coupons/validate')
      .set('Origin', origin)
      .set('Content-Type', 'text/plain')
      .send('x')
      .expect(415);
    // CORS never reflects an untrusted origin and never uses '*'.
    const evil = await request(app)
      .options('/api/coupons/validate')
      .set('Origin', 'https://evil.example')
      .set('Access-Control-Request-Method', 'POST');
    assert.equal(evil.headers['access-control-allow-origin'], undefined);
    const good = await request(app)
      .options('/api/coupons/validate')
      .set('Origin', 'http://127.0.0.1:5173')
      .set('Access-Control-Request-Method', 'POST');
    assert.equal(good.headers['access-control-allow-origin'], 'http://127.0.0.1:5173');
    assert.equal(good.headers['access-control-allow-credentials'], 'true');
    // The stricter refusal for a non-browser client with no cookie is unchanged: it is not a CSRF vector.
    await request(app)
      .post('/api/coupons/validate')
      .send({ code: 'WELCOME150', items: cart })
      .expect(200);
  });

  await t.test('FRONTEND_ORIGINS configuration is validated at startup', async () => {
    const parse = (value, env = {}) =>
      execFileSync(
        'node',
        [
          '--input-type=module',
          '-e',
          "import { origins } from './src/config/env.js'; console.log(JSON.stringify(origins))",
        ],
        {
          env: {
            ...process.env,
            ...(value === undefined ? {} : { FRONTEND_ORIGINS: value }),
            ...env,
          },
          encoding: 'utf8',
          stdio: ['ignore', 'pipe', 'pipe'],
        },
      );
    assert.deepEqual(JSON.parse(parse('http://localhost:5173/, https://app.example.com')), [
      'http://localhost:5173',
      'https://app.example.com',
    ]);
    assert.deepEqual(JSON.parse(parse('')), ['http://localhost:5173', 'http://127.0.0.1:5173']); // development default
    for (const bad of [
      '*',
      'https://*.example.com',
      'localhost:5173',
      'http://localhost:5173/app',
      'ftp://example.com',
    ])
      assert.throws(() => parse(bad), /must be an exact origin/, bad);
    assert.throws(() => parse('', { NODE_ENV: 'production' }), /Set FRONTEND_ORIGINS/); // no implicit default in production
  });

  await t.test(
    'WELCOME150: server prices the cart and ignores forged amounts and discounts',
    async () => {
      const cart = [line(ac, 450)];
      const subtotal = price(cart[0], ac);
      const res = await validate(anon, 'WELCOME150', cart).expect(200);
      assert.equal(res.body.valid, true);
      assert.equal(res.body.subtotal, subtotal);
      assert.equal(res.body.discount, 150);
      assert.equal(res.body.taxes, 0);
      assert.equal(res.body.total, subtotal - 150);
      assert.equal(res.body.coupon.code, 'WELCOME150');
      assert.equal(typeof res.body.coupon.value, 'number');
      assert.equal(res.body.coupon.id, undefined); // internal fields are not exposed
      assert.equal(res.body.coupon.isActive, undefined);
      // Codes are case- and whitespace-insensitive.
      assert.equal((await validate(anon, '  welcome150 ', cart).expect(200)).body.discount, 150);
      // Client-supplied amounts, totals or discounts change nothing.
      const forged = await validate(anon, 'WELCOME150', cart, {
        amount: 1,
        subtotal: 1,
        discount: 9999,
        total: 1,
      }).expect(200);
      assert.deepEqual([forged.body.subtotal, forged.body.discount], [subtotal, 150]);
    },
  );

  await t.test(
    'CLEAN10: category-restricted, 10% capped at 200, only on eligible items',
    async () => {
      const small = [line(first, 550)];
      const eligible = price(small[0], first);
      const r1 = await validate(anon, 'CLEAN10', small).expect(200);
      assert.equal(r1.body.discount, Math.min(Math.round(eligible * 0.1), 200));
      // A big cleaning cart hits the 200 cap.
      const big = [line(first, 4000)];
      if (price(big[0], first) * 0.1 > 200)
        assert.equal((await validate(anon, 'CLEAN10', big).expect(200)).body.discount, 200);
      // Not valid for a lighting-only cart, with a clear reason.
      const wrong = await validate(anon, 'CLEAN10', [line(lighting, 800)]).expect(400);
      assert.match(
        wrong.body.error,
        /CLEAN10 applies only to .*Home Cleaning.* and your cart has none of them/,
      );
      // A mixed cart discounts only the eligible portion, never the whole cart.
      const mixed = [small[0], line(lighting, 800)];
      const r3 = await validate(anon, 'CLEAN10', mixed).expect(200);
      assert.equal(r3.body.subtotal, eligible + price(mixed[1], lighting));
      assert.equal(r3.body.discount, Math.min(Math.round(eligible * 0.1), 200));
    },
  );

  await t.test(
    'invalid, expired, inactive, below-minimum, empty and malformed requests are refused',
    async () => {
      const cart = [line(ac, 450)];
      const past = new Date(Date.now() - 86400000);
      await db.offer.createMany({
        data: [
          {
            code: 'OLD1',
            description: 'expired',
            discountType: 'FLAT',
            value: 50,
            minBookingAmount: 10,
            expiry: past,
          },
          {
            code: 'OFF1',
            description: 'inactive',
            discountType: 'FLAT',
            value: 50,
            minBookingAmount: 10,
            expiry: new Date(Date.now() + 864e5),
            isActive: false,
          },
          {
            code: 'BIG5000',
            description: 'high minimum',
            discountType: 'FLAT',
            value: 50,
            minBookingAmount: 5000,
            expiry: new Date(Date.now() + 864e5),
          },
        ],
      });
      assert.match((await validate(anon, 'NOPE', cart).expect(400)).body.error, /not valid/);
      assert.match((await validate(anon, 'OFF1', cart).expect(400)).body.error, /not valid/);
      assert.match((await validate(anon, 'OLD1', cart).expect(400)).body.error, /expired/);
      assert.match(
        (await validate(anon, 'BIG5000', cart).expect(400)).body.error,
        /at least .*5,000/,
      );
      assert.match((await validate(anon, "' OR 1=1 --", cart).expect(400)).body.error, /not valid/);
      await validate(anon, '', cart).expect(400);
      await validate(anon, 'WELCOME150', []).expect(400);
      await send(anon, 'post', '/api/coupons/validate', { code: 'WELCOME150' }).expect(400);
      await validate(anon, 'WELCOME150', [{ ...cart[0], quantity: 0 }]).expect(400);
      await validate(anon, 'WELCOME150', [{ ...cart[0], quantity: 11 }]).expect(400);
      await validate(anon, 'WELCOME150', [{ serviceId: 'no-such-service', quantity: 1 }]).expect(
        400,
      );
      await validate(anon, 'WELCOME150', [{ ...cart[0], variantId: 'not-a-variant' }]).expect(400);
    },
  );

  await t.test(
    'checkout: discount is recalculated on the server; first-booking coupon is single-use',
    async () => {
      const cart = [line(ac, 450)];
      const subtotal = price(cart[0], ac);
      // The browser cannot dictate the total or the discount.
      const b1 = await book(customer, addressId, cart, {
        couponCode: 'welcome150',
        total: 1,
        discount: 9999,
        subtotal: 1,
      }).expect(201);
      assert.equal(b1.body.booking.total, subtotal - 150);
      assert.equal(b1.body.booking.discount, 150);
      assert.equal(
        (await db.booking.findUnique({ where: { id: b1.body.booking.id } })).couponCode,
        'WELCOME150',
      ); // canonical code
      // Used: this customer's preview and a second booking are refused; nobody else is affected.
      assert.match(
        (await validate(customer, 'WELCOME150', cart).expect(400)).body.error,
        /already used WELCOME150/,
      );
      assert.match(
        (
          await book(
            customer,
            addressId,
            cart,
            { couponCode: 'WELCOME150' },
            '01:00 PM - 02:00 PM',
          ).expect(400)
        ).body.error,
        /already used/,
      );
      await validate(other, 'WELCOME150', cart).expect(200);
      await validate(anon, 'WELCOME150', cart).expect(200); // anonymous preview cannot know; checkout enforces it
      const b2 = await book(other, otherAddressId, cart, { couponCode: 'WELCOME150' }).expect(201);
      assert.equal(b2.body.booking.total, subtotal - 150);
      // Cancelling frees the coupon again.
      await send(customer, 'put', `/api/bookings/${b1.body.booking.id}/cancel`).expect(200);
      await validate(customer, 'WELCOME150', cart).expect(200);
      // Server-side category and validity checks apply to bookings too.
      await book(
        customer,
        addressId,
        [line(lighting, 800)],
        { couponCode: 'CLEAN10' },
        '03:00 PM - 04:00 PM',
      ).expect(400);
      await book(customer, addressId, cart, { couponCode: 'NOPE' }, '03:00 PM - 04:00 PM').expect(
        400,
      );
      const plain = await book(customer, addressId, cart, {}, '05:00 PM - 06:00 PM').expect(201);
      assert.equal(plain.body.booking.discount, 0);
      assert.equal(plain.body.booking.total, subtotal);
    },
  );

  await t.test(
    'public coupon list stays available; a stale or forged session is only anonymous',
    async () => {
      assert.ok((await request(app).get('/api/coupons').expect(200)).body.coupons.length >= 5);
      const cart = [line(ac, 450)];
      // A garbage session cookie with an allowed Origin never grants anything and never breaks the preview.
      await request(app)
        .post('/api/coupons/validate')
        .set('Origin', origin)
        .set('Cookie', 'session=forged.jwt.token')
        .send({ code: 'WELCOME150', items: cart })
        .expect(200);
    },
  );

  await t.test('repeated guessing is rate limited', async () => {
    const cart = [line(ac, 450)];
    const statuses = [];
    for (let i = 0; i < 40; i += 1) statuses.push((await validate(anon, `GUESS${i}`, cart)).status);
    assert.ok(statuses.includes(429), 'expected the coupon limiter to trigger');
    const limited = await validate(anon, 'WELCOME150', cart).expect(429);
    assert.match(limited.body.error, /Too many coupon attempts/);
  });
});
