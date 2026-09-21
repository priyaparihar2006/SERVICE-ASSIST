import 'dotenv/config';
import { test, after } from 'node:test';
import assert from 'node:assert/strict';
import { randomUUID, randomBytes } from 'node:crypto';
import { createServer } from 'node:http';
import { execFileSync } from 'node:child_process';
import bcrypt from 'bcrypt';
import request from 'supertest';
import { io as connectClient } from 'socket.io-client';

if (
  !process.env.TEST_DATABASE_URL ||
  !new URL(process.env.TEST_DATABASE_URL).pathname.endsWith('_test')
)
  throw new Error('Set TEST_DATABASE_URL to a separate database whose name ends in _test');
process.env.DATABASE_URL = process.env.TEST_DATABASE_URL;
process.env.NODE_ENV = 'test';
process.env.CHAT_BLOCK_CONTACT_INFO = 'true';

// Everything the process logs is captured so we can prove message text never reaches the logs.
const logged = [];
for (const level of ['log', 'info', 'warn', 'error']) {
  const original = console[level];
  console[level] = (...args) => {
    logged.push(args.map(String).join(' '));
    original(...args);
  };
}

const { app } = await import('../src/app.js');
const { db } = await import('../src/config/db.js');
const { attachRealtime, SOCKET_PATH } = await import('../src/realtime/socket.js');

const origin = 'http://localhost:5173';
const password = `Test-${randomUUID()}`;
const MARKER = 'zq7-private-marker';
const phones = {
  customer: '9876500001',
  pro: '9876500002',
  other: '9876500003',
  pro2: '9876500004',
};
const emails = {
  customer: 'chat-customer@example.test',
  other: 'chat-other@example.test',
  pro: 'chat-pro@example.test',
  pro2: 'chat-pro2@example.test',
  admin: 'chat-admin@example.test',
};
const agents = {
  customer: request.agent(app),
  other: request.agent(app),
  pro: request.agent(app),
  pro2: request.agent(app),
  admin: request.agent(app),
};
const cookies = {};
const ids = {};
const send = (agent, method, url, body = {}) => agent[method](url).set('Origin', origin).send(body);
const sessionCookie = (res) => res.headers['set-cookie'][0].split(';')[0];
const uuid = () => randomUUID();
const containsAny = (value, needles) => needles.filter((n) => JSON.stringify(value).includes(n));

const httpServer = createServer(app);
const realtime = attachRealtime(httpServer);
await new Promise((resolve) => httpServer.listen(0, '127.0.0.1', resolve));
const baseUrl = `http://127.0.0.1:${httpServer.address().port}`;
const sockets = [];
const connect = (cookie, { origin: o = origin, auth } = {}) =>
  new Promise((resolve, reject) => {
    const socket = connectClient(baseUrl, {
      path: SOCKET_PATH,
      transports: ['websocket'],
      forceNew: true,
      reconnection: false,
      auth,
      extraHeaders: { ...(cookie ? { cookie } : {}), ...(o ? { origin: o } : {}) },
    });
    sockets.push(socket);
    socket.once('connect', () => resolve(socket));
    socket.once('connect_error', (error) => {
      socket.close();
      reject(error);
    });
  });
const waitFor = (socket, event, ms = 4000) =>
  new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error(`Timed out waiting for ${event}`)), ms);
    socket.once(event, (payload) => {
      clearTimeout(timer);
      resolve(payload);
    });
  });
const stayQuiet = (socket, event, ms = 500) =>
  new Promise((resolve, reject) => {
    const handler = () => reject(new Error(`Unexpected ${event}`));
    socket.once(event, handler);
    setTimeout(() => {
      socket.off(event, handler);
      resolve();
    }, ms);
  });
const spy = (socket, events) => {
  const seen = [];
  for (const event of events) socket.on(event, (payload) => seen.push({ event, payload }));
  return seen;
};
const emitWithAck = (socket, event, payload) =>
  new Promise((resolve) =>
    socket.timeout(3000).emit(event, payload, (err, res) => resolve(err ? { timeout: true } : res)),
  );

after(async () => {
  for (const s of sockets) s.close();
  await realtime.close();
  await db.$disconnect();
});

const date = new Date(Date.now() + 3 * 86400000).toISOString().slice(0, 10);
let service;
const addressIds = {};
async function newBooking(slot, who = 'customer') {
  const addressId = addressIds[who];
  const res = await send(agents[who], 'post', '/api/bookings', {
    addressId,
    items: [{ serviceId: service.id, variantId: service.variants[0].id, quantity: 1 }],
    bookingDate: date,
    bookingTime: slot,
    paymentMethod: 'CASH',
  })
    .set('Idempotency-Key', uuid())
    .expect(201);
  return res.body.booking;
}
const assign = (bookingId, professionalId) =>
  send(agents.admin, 'put', `/api/bookings/${bookingId}/status`, {
    status: 'ASSIGNED',
    professionalId,
  });

test('private chat: privacy, access control, encryption, realtime and audit', async (t) => {
  await db.$executeRawUnsafe('TRUNCATE "User", "Category", "Offer" RESTART IDENTITY CASCADE');
  process.env.SEED_DEMO = 'true';
  process.env.DEMO_PASSWORD = password;
  await import('../prisma/seed.js');
  service = (await request(app).get('/api/services/srv-ac-foamjet').expect(200)).body.service;

  // ---- Fixtures: separate customer, professional and administrator accounts ----
  const register = async (key, name, path = '/api/auth/register', extra = {}) => {
    const res = await send(agents[key], 'post', path, {
      name,
      email: emails[key],
      phone: phones[key],
      password,
      ...extra,
    }).expect(201);
    cookies[key] = sessionCookie(res);
    ids[key] = res.body.user.id;
  };
  await register('customer', 'Chat Customer');
  await register('other', 'Other Customer');
  await register('pro', 'Chat Plumber', '/api/professionals/register');
  await register('pro2', 'Second Plumber', '/api/professionals/register');
  await db.user.create({
    data: {
      name: 'Chat Admin',
      email: emails.admin,
      passwordHash: await bcrypt.hash(password, 12),
      role: 'ADMIN',
    },
  });
  cookies.admin = sessionCookie(
    await send(agents.admin, 'post', '/api/auth/login', { email: emails.admin, password }).expect(
      200,
    ),
  );
  const proIds = {};
  for (const key of ['pro', 'pro2']) {
    proIds[key] = (
      await agents[key].get('/api/professionals/profile').expect(200)
    ).body.professional.id;
    await send(agents[key], 'put', '/api/professionals/profile', {
      businessName: key === 'pro' ? 'Chat Plumbing Works' : 'Second Plumbing Works',
      serviceIds: [service.id],
      serviceArea: ['Agra'],
      isAvailableToday: true,
    }).expect(200);
    await send(agents[key], 'put', '/api/professionals/availability', {
      availability: Array.from({ length: 7 }, (_, dayOfWeek) => ({
        dayOfWeek,
        startMinute: 480,
        endMinute: 1140,
      })),
    }).expect(200);
    await send(agents.admin, 'put', `/api/admin/professionals/${proIds[key]}/verification`, {
      verificationStatus: 'VERIFIED',
    }).expect(200);
  }
  for (const who of ['customer', 'other'])
    addressIds[who] = (
      await send(agents[who], 'post', '/api/addresses', {
        house: '10',
        street: 'Chat Road',
        city: 'Agra',
        state: 'Uttar Pradesh',
        pincode: '282001',
        isDefault: true,
      }).expect(201)
    ).body.address.id;
  const pending = await newBooking('09:00 AM - 10:00 AM');
  const main = await newBooking('11:00 AM - 12:00 PM');
  await assign(main.id, proIds.pro).expect(200);
  await send(agents.pro, 'put', `/api/professionals/bookings/${main.id}/status`, {
    status: 'CONFIRMED',
  }).expect(200);
  const userIdOf = async (email) => (await db.user.findUnique({ where: { email } })).id;
  const proUserId = await userIdOf(emails.pro);
  let conversationId;

  await t.test(
    "booking APIs never reveal either party's phone number or e-mail to the other",
    async () => {
      const asCustomer = (await agents.customer.get(`/api/bookings/${main.id}`).expect(200)).body;
      const asPro = (await agents.pro.get(`/api/bookings/${main.id}`).expect(200)).body;
      const proList = (await agents.pro.get('/api/professionals/bookings').expect(200)).body;
      const custList = (await agents.customer.get('/api/bookings').expect(200)).body;
      assert.deepEqual(containsAny([asCustomer, custList], [phones.pro, emails.pro]), []);
      assert.deepEqual(containsAny([asPro, proList], [phones.customer, emails.customer]), []);
      assert.equal(asCustomer.booking.professionalPhone, undefined);
      assert.equal(asPro.booking.userPhone, undefined);
      assert.equal(asPro.booking.customerPhone, undefined);
      assert.equal(asPro.booking.userEmail, undefined);
      // Administrators keep contact details for support and safety work.
      const asAdmin = (await agents.admin.get(`/api/bookings/${main.id}`).expect(200)).body.booking;
      assert.equal(asAdmin.userPhone, phones.customer);
      assert.equal(asAdmin.professionalPhone, phones.pro);
    },
  );

  await t.test(
    "conversation creation is limited to the booking's own customer and professional",
    async () => {
      await request(app)
        .post('/api/conversations')
        .set('Origin', origin)
        .send({ bookingId: main.id })
        .expect(401);
      await send(agents.other, 'post', '/api/conversations', { bookingId: main.id }).expect(404);
      await send(agents.pro2, 'post', '/api/conversations', { bookingId: main.id }).expect(404);
      await send(agents.admin, 'post', '/api/conversations', { bookingId: main.id }).expect(403);
      await send(agents.customer, 'post', '/api/conversations', { bookingId: pending.id }).expect(
        409,
      ); // no professional yet
      await send(agents.customer, 'post', '/api/conversations', { bookingId: 'bad id!' }).expect(
        400,
      );
      const created = await send(agents.customer, 'post', '/api/conversations', {
        bookingId: main.id,
      }).expect(201);
      conversationId = created.body.conversation.id;
      const again = await send(agents.pro, 'post', '/api/conversations', {
        bookingId: main.id,
      }).expect(200);
      assert.equal(again.body.conversation.id, conversationId); // one thread per booking
      const rows = await db.conversationParticipant.findMany({ where: { conversationId } });
      assert.deepEqual(rows.map((r) => r.userId).sort(), [ids.customer, proUserId].sort());
    },
  );

  await t.test(
    'conversation payloads expose display data only: no phone, e-mail or user ids',
    async () => {
      const asCustomer = (
        await agents.customer.get(`/api/conversations/${conversationId}`).expect(200)
      ).body.conversation;
      const asPro = (await agents.pro.get(`/api/conversations/${conversationId}`).expect(200)).body
        .conversation;
      const list = (await agents.customer.get('/api/conversations').expect(200)).body;
      const everything = [asCustomer, asPro, list];
      assert.deepEqual(
        containsAny(everything, [
          ...Object.values(phones),
          ...Object.values(emails),
          ids.customer,
          proUserId,
        ]),
        [],
      );
      assert.equal(asCustomer.counterpart.displayName, 'Chat Plumber');
      assert.equal(asCustomer.counterpart.role, 'PROFESSIONAL');
      assert.equal(asPro.counterpart.displayName, 'Chat Customer');
      assert.ok(asCustomer.booking.categoryName && asCustomer.booking.reference === main.id);
      assert.equal(asCustomer.canSend, true);
      assert.equal(
        (await agents.customer.get(`/api/conversations/${conversationId}`)).headers[
          'cache-control'
        ],
        'no-store',
      );
    },
  );

  await t.test('non-members, other roles and guessed ids get nothing', async () => {
    const guess = await agents.other.get(`/api/conversations/${uuid()}`).expect(404);
    const foreign = await agents.other.get(`/api/conversations/${conversationId}`).expect(404);
    assert.deepEqual(guess.body, foreign.body); // a real thread is indistinguishable from a missing one
    await agents.pro2.get(`/api/conversations/${conversationId}/messages`).expect(404);
    await send(agents.other, 'post', `/api/conversations/${conversationId}/messages`, {
      content: 'hi',
      clientMessageId: uuid(),
    }).expect(404);
    await send(agents.other, 'put', `/api/conversations/${conversationId}/read`).expect(404);
    await send(agents.other, 'put', `/api/conversations/${conversationId}/block`).expect(404);
    await send(agents.other, 'post', `/api/conversations/${conversationId}/report`, {
      reason: 'SPAM',
    }).expect(404);
    await agents.admin.get(`/api/conversations/${conversationId}`).expect(403);
    await agents.admin.get('/api/conversations').expect(403);
    await agents.admin.get(`/api/conversations/${conversationId}/messages`).expect(403);
    await request(app).get(`/api/conversations/${conversationId}/messages`).expect(401);
    await agents.customer.get('/api/conversations/not-a-uuid').expect(400);
    assert.equal(
      (await agents.other.get('/api/conversations').expect(200)).body.conversations.length,
      0,
    );
  });

  let first;
  await t.test(
    'sending: sender comes from the session, text is validated, retries are idempotent',
    async () => {
      const clientMessageId = uuid();
      const res = await send(
        agents.customer,
        'post',
        `/api/conversations/${conversationId}/messages`,
        {
          content: `  Hello ${MARKER} — are you coming today?  `,
          clientMessageId,
          senderId: proUserId, // forged; must be ignored
          conversationId: uuid(),
        },
      ).expect(201);
      first = res.body.message;
      assert.equal(first.isMine, true);
      assert.equal(first.status, 'sent');
      assert.equal(first.content, `Hello ${MARKER} — are you coming today?`);
      assert.equal(first.senderId, undefined);
      const stored = await db.message.findUnique({ where: { id: first.id } });
      assert.equal(stored.senderId, ids.customer);
      assert.equal(stored.conversationId, conversationId);
      // Retrying the same client id returns the same message instead of duplicating it.
      const retry = await send(
        agents.customer,
        'post',
        `/api/conversations/${conversationId}/messages`,
        {
          content: `Hello ${MARKER} — are you coming today?`,
          clientMessageId,
        },
      ).expect(201);
      assert.equal(retry.body.message.id, first.id);
      assert.equal(await db.message.count({ where: { conversationId } }), 1);
      const url = `/api/conversations/${conversationId}/messages`;
      for (const body of [
        { content: '', clientMessageId: uuid() },
        { content: '   ', clientMessageId: uuid() },
        { content: 'x'.repeat(2001), clientMessageId: uuid() },
        { content: 'bad \u0000 byte', clientMessageId: uuid() },
        { content: `spoof ${String.fromCharCode(0x202e)} text`, clientMessageId: uuid() },
        { content: 'no client id' },
        { content: 'bad client id', clientMessageId: 'nope' },
        { content: 123, clientMessageId: uuid() },
      ])
        await send(agents.customer, 'post', url, body).expect(400);
      await agents.customer
        .post(url)
        .set('Origin', 'https://evil.test')
        .send({ content: 'x', clientMessageId: uuid() })
        .expect(403);
      await agents.customer.post(url).send({ content: 'x', clientMessageId: uuid() }).expect(403); // cookie without Origin
      await agents.customer
        .post(url)
        .set('Origin', origin)
        .set('Content-Type', 'text/plain')
        .send('x')
        .expect(415);
      const view = (await agents.customer.get(url).expect(200)).body.messages;
      assert.deepEqual(
        view.map((m) => m.status),
        ['sent'],
      ); // the professional has not fetched anything yet
    },
  );

  await t.test('phone numbers and e-mail addresses cannot be typed into a message', async () => {
    const url = `/api/conversations/${conversationId}/messages`;
    for (const content of [
      'call me 98765 43210',
      'my number is +91-98765-43210',
      'mail me at a.b@gmail.com',
      'reach me: 9876543210',
    ]) {
      const res = await send(agents.customer, 'post', url, {
        content,
        clientMessageId: uuid(),
      }).expect(422);
      assert.equal(res.body.code, 'CONTACT_INFO_NOT_ALLOWED');
    }
    await send(agents.customer, 'post', url, {
      content: 'Please arrive by 4821 rupees budget 1500, pin 282001',
      clientMessageId: uuid(),
    }).expect(201);
    assert.equal(await db.message.count({ where: { conversationId } }), 2);
  });

  await t.test('stored content is authenticated ciphertext bound to its row', async () => {
    const rows = (
      await db.message.findMany({ where: { conversationId }, orderBy: { createdAt: 'asc' } })
    ).map((row) => ({ ...row, encryptedContent: Buffer.from(row.encryptedContent) }));
    for (const row of rows) {
      assert.ok(row.encryptedContent.length > 28);
      assert.ok(!row.encryptedContent.includes(Buffer.from(MARKER)), 'plaintext found in database');
      assert.equal(row.encryptionVersion, 1);
    }
    const [a, b] = rows;
    // 1) Bit-flip: the message is reported as undecryptable, not silently altered.
    const flipped = Buffer.from(a.encryptedContent);
    flipped[flipped.length - 1] ^= 1;
    await db.message.update({ where: { id: a.id }, data: { encryptedContent: flipped } });
    let page = (await agents.pro.get(`/api/conversations/${conversationId}/messages`).expect(200))
      .body.messages;
    assert.match(page.find((m) => m.id === a.id).content, /could not be decrypted/);
    // 2) Swap: ciphertext moved to another message row fails authentication too.
    await db.message.update({
      where: { id: a.id },
      data: { encryptedContent: b.encryptedContent },
    });
    page = (await agents.pro.get(`/api/conversations/${conversationId}/messages`).expect(200)).body
      .messages;
    assert.match(page.find((m) => m.id === a.id).content, /could not be decrypted/);
    await db.message.update({
      where: { id: a.id },
      data: { encryptedContent: a.encryptedContent },
    });
    page = (await agents.pro.get(`/api/conversations/${conversationId}/messages`).expect(200)).body
      .messages;
    assert.equal(page.find((m) => m.id === a.id).content, first.content);
    // The data key is stored wrapped, and the database refuses inconsistent ciphertext state.
    const conversation = await db.conversation.findUnique({ where: { id: conversationId } });
    assert.equal(conversation.wrappedKey.length, 12 + 16 + 32); // iv + tag + 256-bit data key
    await assert.rejects(
      db.$executeRaw`UPDATE "Message" SET "encryptedContent" = NULL WHERE id = ${a.id}`,
    );
  });

  await t.test('delivery and read receipts, unread counts and history paging', async () => {
    assert.equal((await agents.pro.get('/api/conversations/unread').expect(200)).body.unread, 2);
    const forPro = (
      await agents.pro.get(`/api/conversations/${conversationId}/messages`).expect(200)
    ).body.messages;
    assert.deepEqual(
      forPro.map((m) => m.isMine),
      [false, false],
    );
    assert.equal(forPro[0].status, undefined);
    const delivered = (
      await agents.customer.get(`/api/conversations/${conversationId}/messages`).expect(200)
    ).body.messages;
    assert.equal(delivered[0].status, 'delivered'); // fetching history marked them delivered
    const list = (await agents.pro.get('/api/conversations').expect(200)).body.conversations[0];
    assert.equal(list.unreadCount, 2);
    assert.match(list.lastMessage.preview, /Please arrive/);
    const read = await send(agents.pro, 'put', `/api/conversations/${conversationId}/read`).expect(
      200,
    );
    assert.equal(read.body.read, 2);
    assert.equal((await agents.pro.get('/api/conversations/unread')).body.unread, 0);
    const seen = (
      await agents.customer.get(`/api/conversations/${conversationId}/messages`).expect(200)
    ).body.messages;
    assert.deepEqual(
      seen.map((m) => m.status),
      ['read', 'read'],
    );
    const receipt = await db.messageReceipt.findFirst({ where: { messageId: first.id } });
    assert.ok(receipt.deliveredAt && receipt.readAt && receipt.userId === proUserId);
    assert.ok(
      (await db.conversationParticipant.findFirst({ where: { conversationId, userId: proUserId } }))
        .lastReadAt,
    );
    const paged = (
      await agents.customer.get(`/api/conversations/${conversationId}/messages?limit=1`).expect(200)
    ).body;
    assert.equal(paged.messages.length, 1);
    assert.equal(paged.hasMore, true);
    const older = (
      await agents.customer
        .get(`/api/conversations/${conversationId}/messages?limit=1&before=${paged.nextBefore}`)
        .expect(200)
    ).body;
    assert.equal(older.messages[0].id, first.id);
    assert.equal(older.hasMore, false);
    await agents.customer
      .get(`/api/conversations/${conversationId}/messages?before=${uuid()}`)
      .expect(400);
  });

  await t.test(
    'WebSocket authentication: no cookie, forged cookie, wrong or missing origin, admin are all refused',
    async () => {
      await assert.rejects(connect(null), /Unauthorized/);
      await assert.rejects(connect('session=forged.jwt.value'), /Unauthorized/);
      await assert.rejects(
        connect(cookies.customer, { origin: 'https://evil.test' }),
        /Origin is not allowed|Unauthorized|websocket error/i,
      );
      await assert.rejects(
        connect(cookies.customer, { origin: null }),
        /Origin is not allowed|websocket error/i,
      );
      await assert.rejects(connect(cookies.admin), /Connection refused/);
      const spoof = await connect(cookies.customer, {
        auth: { userId: proUserId, token: 'ignored' },
      });
      assert.equal(spoof.connected, true); // extra client-supplied identity fields are simply ignored
      spoof.close();
    },
  );

  await t.test(
    'real-time delivery, receipts, typing and presence reach only the right participants',
    async () => {
      const proSocket = await connect(cookies.pro);
      const otherSocket = await connect(cookies.other);
      const outsiderPro = await connect(cookies.pro2);
      const spoofed = await connect(cookies.other, { auth: { userId: ids.customer } });
      const events = [
        'message:new',
        'typing',
        'message:status',
        'presence',
        'message:deleted',
        'conversation:updated',
        'booking:updated',
      ];
      const leaks = [otherSocket, outsiderPro, spoofed].map((s) => spy(s, events));

      // Presence: the counterpart hears when a user comes online, not for extra tabs, and when they leave.
      const cameOnline = waitFor(proSocket, 'presence');
      const customerSocket = await connect(cookies.customer);
      assert.deepEqual(await cameOnline, { conversationId, online: true });
      const secondTab = await connect(cookies.customer);
      secondTab.close();
      await stayQuiet(proSocket, 'presence', 400); // still online through the first tab
      const wentOffline = waitFor(proSocket, 'presence');
      customerSocket.close();
      assert.deepEqual(await wentOffline, { conversationId, online: false });
      const cameBack = waitFor(proSocket, 'presence');
      const customerAgain = await connect(cookies.customer);
      assert.deepEqual(await cameBack, { conversationId, online: true });
      const createdUpdate = waitFor(customerAgain, 'booking:updated');
      const dispatch = await newBooking('04:00 PM - 05:00 PM');
      assert.deepEqual(await createdUpdate, { bookingId: dispatch.id, status: 'PENDING' });
      const assignedCustomer = waitFor(customerAgain, 'booking:updated');
      const assignedProfessional = waitFor(proSocket, 'booking:updated');
      const professionalNotice = waitFor(proSocket, 'notification:new');
      await assign(dispatch.id, proIds.pro).expect(200);
      assert.deepEqual(await assignedCustomer, { bookingId: dispatch.id, status: 'ASSIGNED' });
      assert.deepEqual(await assignedProfessional, { bookingId: dispatch.id, status: 'ASSIGNED' });
      assert.deepEqual(await professionalNotice, { bookingId: dispatch.id });
      const acceptedCustomer = waitFor(customerAgain, 'booking:updated');
      await send(agents.pro, 'post', `/api/bookings/${dispatch.id}/accept`).expect(200);
      assert.deepEqual(await acceptedCustomer, { bookingId: dispatch.id, status: 'CONFIRMED' });
      assert.equal(leaks.flat().filter((e) => e.event === 'booking:updated').length, 0);
      assert.equal(
        (await agents.customer.get(`/api/conversations/${conversationId}`)).body.conversation
          .counterpart.online,
        true,
      );

      const incoming = waitFor(proSocket, 'message:new');
      const echo = waitFor(customerAgain, 'message:new');
      const sent = await send(
        agents.customer,
        'post',
        `/api/conversations/${conversationId}/messages`,
        {
          content: `Realtime ${MARKER}`,
          clientMessageId: uuid(),
        },
      ).expect(201);
      const push = await incoming;
      assert.equal(push.conversationId, conversationId);
      assert.equal(push.message.content, `Realtime ${MARKER}`);
      assert.equal(push.message.isMine, false);
      assert.equal((await echo).message.isMine, true);

      const delivered = waitFor(customerAgain, 'message:status');
      assert.deepEqual(
        await emitWithAck(proSocket, 'message:delivered', {
          conversationId,
          messageIds: [sent.body.message.id],
        }),
        { ok: true },
      );
      const deliveredStatus = await delivered;
      assert.equal(deliveredStatus.status, 'delivered');
      assert.deepEqual(deliveredStatus.messageIds, [sent.body.message.id]);
      // A stranger cannot acknowledge (or probe) someone else's messages.
      assert.deepEqual(
        await emitWithAck(outsiderPro, 'message:delivered', {
          conversationId,
          messageIds: [sent.body.message.id],
        }),
        { ok: false, error: 'forbidden' },
      );

      const typing = waitFor(proSocket, 'typing');
      assert.deepEqual(
        await emitWithAck(customerAgain, 'typing', { conversationId, isTyping: true }),
        { ok: true },
      );
      assert.deepEqual(await typing, { conversationId, isTyping: true });
      assert.deepEqual(
        await emitWithAck(outsiderPro, 'typing', { conversationId, isTyping: true }),
        { ok: false, error: 'forbidden' },
      );
      assert.deepEqual(
        await emitWithAck(customerAgain, 'typing', { conversationId: 'nope', isTyping: true }),
        { ok: false, error: 'invalid' },
      );
      assert.deepEqual(
        await emitWithAck(customerAgain, 'typing', { conversationId, isTyping: 'yes' }),
        { ok: false, error: 'invalid' },
      );

      const read = waitFor(customerAgain, 'message:status');
      await send(agents.pro, 'put', `/api/conversations/${conversationId}/read`).expect(200);
      const status = await read;
      assert.equal(status.status, 'read');
      assert.ok(status.messageIds.includes(sent.body.message.id));

      await new Promise((resolve) => setTimeout(resolve, 300));
      assert.deepEqual(leaks.flat(), []); // nothing reached sockets that are not part of the conversation
      otherSocket.close();
      outsiderPro.close();
      spoofed.close();

      // Logging out (or changing the password) revokes live sockets immediately.
      const closed = new Promise((resolve) => customerAgain.once('disconnect', resolve));
      await send(agents.customer, 'post', '/api/auth/logout').expect(200);
      assert.ok(['io server disconnect', 'transport close'].includes(await closed));
      await assert.rejects(connect(cookies.customer), /Unauthorized/);
      proSocket.close();
      cookies.customer = sessionCookie(
        await send(agents.customer, 'post', '/api/auth/login', {
          email: emails.customer,
          password,
        }).expect(200),
      );
    },
  );

  await t.test('connection cap per user and typing/ack flood limits', async () => {
    const open = [];
    for (let i = 0; i < 5; i += 1) open.push(await connect(cookies.other));
    await assert.rejects(connect(cookies.other), /Connection refused/);
    const results = [];
    for (let i = 0; i < 60; i += 1)
      results.push(
        await emitWithAck(open[0], 'typing', { conversationId: uuid(), isTyping: true }),
      );
    assert.ok(results.some((r) => r.error === 'rate_limited'));
    open.forEach((s) => s.close());
  });

  await t.test(
    'deleting a message: own messages only, content erased, blocked during a report',
    async () => {
      const url = `/api/conversations/${conversationId}/messages`;
      const mine = (
        await send(agents.customer, 'post', url, {
          content: `Delete me ${MARKER}`,
          clientMessageId: uuid(),
        }).expect(201)
      ).body.message;
      await send(agents.pro, 'delete', `/api/messages/${mine.id}`).expect(403); // not theirs
      await send(agents.other, 'delete', `/api/messages/${mine.id}`).expect(404);
      await send(agents.customer, 'delete', `/api/messages/${uuid()}`).expect(404);
      await send(agents.customer, 'delete', `/api/messages/${mine.id}`).expect(200);
      const row = await db.message.findUnique({ where: { id: mine.id } });
      assert.equal(row.encryptedContent, null);
      assert.ok(row.deletedAt);
      const seen = (await agents.pro.get(url).expect(200)).body.messages.find(
        (m) => m.id === mine.id,
      );
      assert.deepEqual([seen.deleted, seen.content], [true, null]);
      await send(agents.customer, 'delete', `/api/messages/${mine.id}`).expect(200); // idempotent
    },
  );

  await t.test(
    'blocking: the blocker sees why, the blocked person learns nothing specific',
    async () => {
      const url = `/api/conversations/${conversationId}/messages`;
      const blocked = await send(
        agents.customer,
        'put',
        `/api/conversations/${conversationId}/block`,
      ).expect(200);
      assert.equal(blocked.body.conversation.blockedByMe, true);
      assert.equal(blocked.body.conversation.sendBlockedReason, 'BLOCKED_BY_YOU');
      await send(agents.customer, 'post', url, {
        content: 'hello',
        clientMessageId: uuid(),
      }).expect(403);
      const theirs = await send(agents.pro, 'post', url, {
        content: 'hello?',
        clientMessageId: uuid(),
      }).expect(403);
      assert.doesNotMatch(theirs.body.error, /block/i);
      const view = (await agents.pro.get(`/api/conversations/${conversationId}`).expect(200)).body
        .conversation;
      assert.deepEqual([view.blockedByMe, view.sendBlockedReason], [false, 'UNAVAILABLE']);
      await send(agents.customer, 'delete', `/api/conversations/${conversationId}/block`).expect(
        200,
      );
      await send(agents.pro, 'post', url, {
        content: 'Back on my way',
        clientMessageId: uuid(),
      }).expect(201);
    },
  );

  await t.test('reporting and the administrator access policy', async () => {
    const report = await send(agents.pro, 'post', `/api/conversations/${conversationId}/report`, {
      reason: 'OFF_PLATFORM_CONTACT',
      details: 'Asked to move off the app',
    }).expect(201);
    await send(agents.pro, 'post', `/api/conversations/${conversationId}/report`, {
      reason: 'SPAM',
    }).expect(409);
    await send(agents.pro, 'post', `/api/conversations/${conversationId}/report`, {
      reason: 'NOPE',
    }).expect(400);
    // While the report is open, evidence cannot be deleted.
    const evidence = (
      await agents.pro.get(`/api/conversations/${conversationId}/messages`)
    ).body.messages.at(-1);
    await send(agents.pro, 'delete', `/api/messages/${evidence.id}`).expect(409);
    // Only administrators can reach the review endpoints, and only with a justification.
    for (const key of ['customer', 'pro', 'other'])
      await agents[key].get('/api/admin/chat/reports').expect(403);
    await request(app).get('/api/admin/chat/reports').expect(401);
    const listed = (await agents.admin.get('/api/admin/chat/reports?status=OPEN').expect(200)).body
      .reports;
    assert.equal(listed.length, 1);
    assert.deepEqual(containsAny(listed, [...Object.values(phones), ...Object.values(emails)]), []);
    const readUrl = `/api/admin/chat/reports/${report.body.report.id}/messages`;
    await send(agents.admin, 'post', readUrl, {}).expect(400);
    await send(agents.admin, 'post', readUrl, { justification: 'too short' }).expect(400);
    await send(agents.pro, 'post', readUrl, { justification: 'I am not an administrator' }).expect(
      403,
    );
    assert.equal(await db.chatAccessLog.count(), 0);
    const opened = (
      await send(agents.admin, 'post', readUrl, {
        justification: 'Reviewing reported off-platform request',
      }).expect(200)
    ).body;
    assert.ok(opened.messages.some((m) => m.content?.includes(MARKER)));
    assert.ok(opened.messages.every((m) => ['CUSTOMER', 'PROFESSIONAL'].includes(m.senderRole)));
    assert.deepEqual(containsAny(opened, [...Object.values(phones), ...Object.values(emails)]), []);
    const log = await db.chatAccessLog.findMany();
    assert.equal(log.length, 1);
    assert.equal(log[0].conversationId, conversationId);
    assert.equal(log[0].messageCount, opened.messages.length);
    assert.match(log[0].justification, /off-platform/);
    // The audit trail is append-only, even for someone with direct database access.
    await assert.rejects(
      db.$executeRaw`UPDATE "ChatAccessLog" SET "justification" = 'edited'`,
      /append-only/,
    );
    await assert.rejects(db.$executeRaw`DELETE FROM "ChatAccessLog"`, /append-only/);
    assert.equal(
      (await agents.admin.get('/api/admin/chat/access-log').expect(200)).body.entries.length,
      1,
    );
    // Resolving the report ends the administrator's access and re-enables deletion.
    await send(agents.admin, 'put', `/api/admin/chat/reports/${report.body.report.id}`, {
      status: 'RESOLVED',
      resolutionNote: 'Warned the customer',
    }).expect(200);
    await send(agents.admin, 'post', readUrl, {
      justification: 'Trying again after resolution',
    }).expect(409);
    await send(agents.admin, 'put', `/api/admin/chat/reports/${report.body.report.id}`, {
      status: 'DISMISSED',
      resolutionNote: 'again',
    }).expect(409);
    await send(agents.pro, 'delete', `/api/messages/${evidence.id}`).expect(200);
  });

  await t.test('offline recipients get one content-free notification per booking', async () => {
    const notes = await db.notification.findMany({
      where: { userId: { in: [ids.customer, proUserId] }, type: 'CHAT' },
    });
    assert.ok(notes.length >= 1 && notes.length <= 2);
    for (const n of notes) {
      assert.ok(!n.message.includes(MARKER) && !n.title.includes(MARKER));
      assert.match(n.message, new RegExp(main.id));
    }
  });

  await t.test(
    'reassignment closes the old thread: previous professional loses all access',
    async () => {
      const second = await newBooking('01:00 PM - 02:00 PM');
      const candidateIds = (await agents.admin.get(`/api/admin/bookings/${second.id}/eligible-professionals`).expect(200)).body.professionals.map((p) => p.id);
      assert.ok(candidateIds.includes(proIds.pro) && candidateIds.includes(proIds.pro2));
      await assign(second.id, proIds.pro).expect(200);
      await send(agents.pro2, 'post', `/api/bookings/${second.id}/accept`).expect(404);
      const convo = (
        await send(agents.customer, 'post', '/api/conversations', { bookingId: second.id }).expect(
          201,
        )
      ).body.conversation;
      await send(agents.customer, 'post', `/api/conversations/${convo.id}/messages`, {
        content: `Before reassignment ${MARKER}`,
        clientMessageId: uuid(),
      }).expect(201);
      await agents.pro.get(`/api/conversations/${convo.id}/messages`).expect(200);
      // The professional declines; the admin assigns someone else.
      await send(agents.pro, 'post', `/api/bookings/${second.id}/reject`).expect(200);
      assert.equal((await agents.pro.get('/api/professionals/rejected-bookings').expect(200)).body.requests[0].bookingId, second.id);
      await agents.pro.get(`/api/conversations/${convo.id}`).expect(404);
      await agents.pro.get(`/api/conversations/${convo.id}/messages`).expect(404);
      await send(agents.pro, 'post', `/api/conversations/${convo.id}/messages`, {
        content: 'still here?',
        clientMessageId: uuid(),
      }).expect(404);
      assert.equal(
        (await agents.pro.get('/api/conversations').expect(200)).body.conversations.some(
          (c) => c.id === convo.id,
        ),
        false,
      );
      const kept = (await agents.customer.get(`/api/conversations/${convo.id}`).expect(200)).body
        .conversation;
      assert.deepEqual(
        [kept.status, kept.canSend, kept.sendBlockedReason],
        ['CLOSED', false, 'CONVERSATION_CLOSED'],
      );
      await send(agents.customer, 'post', `/api/conversations/${convo.id}/messages`, {
        content: 'anyone?',
        clientMessageId: uuid(),
      }).expect(403);
      assert.match(
        (await agents.customer.get(`/api/conversations/${convo.id}/messages`).expect(200)).body
          .messages[0].content,
        /Before reassignment/,
      );
      await assign(second.id, proIds.pro2).expect(200);
      const fresh = (
        await send(agents.customer, 'post', '/api/conversations', { bookingId: second.id }).expect(
          201,
        )
      ).body.conversation;
      assert.notEqual(fresh.id, convo.id);
      assert.equal(fresh.counterpart.displayName, 'Second Plumber');
      assert.equal(
        (await agents.pro2.get(`/api/conversations/${fresh.id}/messages`).expect(200)).body.messages
          .length,
        0,
      ); // no old history
      await agents.pro2.get(`/api/conversations/${convo.id}`).expect(404);
      await agents.pro.get(`/api/conversations/${fresh.id}`).expect(404);
    },
  );

  await t.test(
    'hostile input: SQL and HTML payloads are stored inertly and never executed',
    async () => {
      const url = `/api/conversations/${conversationId}/messages`;
      const payloads = [
        `'); DROP TABLE "Message"; -- ${MARKER}`,
        `" OR 1=1 --`,
        `<img src=x onerror="window.__xss=1"> <script>alert(1)</script>`,
        `{{7*7}} \${process.env.JWT_SECRET} %s %n`,
      ];
      for (const content of payloads) {
        const res = await send(agents.pro, 'post', url, {
          content,
          clientMessageId: uuid(),
        }).expect(201);
        assert.equal(res.body.message.content, content); // returned verbatim: escaping is the renderer's job
      }
      const seen = (await agents.customer.get(url).expect(200)).body.messages.map((m) => m.content);
      for (const content of payloads) assert.ok(seen.includes(content));
      assert.ok((await db.message.count()) > 0); // the table is still there
      // Identifiers and query parameters are validated before they can reach a query.
      for (const bad of ["' OR 1=1 --", '1;DROP TABLE "User"', '../../etc/passwd', '%00'])
        await agents.customer.get(`${url}?before=${encodeURIComponent(bad)}`).expect(400);
      await agents.customer.get(`${url}?limit=1;DROP`).expect(400);
      await agents.customer
        .get(`/api/conversations/${encodeURIComponent("' OR 1=1 --")}`)
        .expect(400);
      await send(agents.customer, 'post', '/api/conversations', {
        bookingId: "x' OR '1'='1",
      }).expect(400);
      assert.ok((await db.user.count()) >= 5);
    },
  );

  await t.test(
    'changing the password revokes open sockets and old sessions immediately',
    async () => {
      const temp = request.agent(app);
      const res = await send(temp, 'post', '/api/auth/register', {
        name: 'Temp Customer',
        email: 'chat-temp@example.test',
        password,
      }).expect(201);
      const socket = await connect(sessionCookie(res));
      const closed = new Promise((resolve) => socket.once('disconnect', resolve));
      await send(temp, 'post', '/api/auth/change-password', {
        currentPassword: password,
        password: `${password}-new`,
      }).expect(200);
      assert.ok(['io server disconnect', 'transport close'].includes(await closed));
      await assert.rejects(connect(sessionCookie(res)), /Unauthorized/);
      await temp.get('/api/conversations').expect(401);
    },
  );

  await t.test('finished bookings stay readable but become read-only', async () => {
    await send(agents.pro, 'put', `/api/bookings/${main.id}/status`, {
      status: 'ON_THE_WAY',
    }).expect(200);
    await send(agents.pro, 'put', `/api/bookings/${main.id}/status`, { status: 'ARRIVED' }).expect(
      200,
    );
    await send(agents.pro, 'put', `/api/bookings/${main.id}/status`, {
      status: 'IN_PROGRESS',
      otp: main.verificationOtp,
    }).expect(200);
    await send(agents.pro, 'post', `/api/conversations/${conversationId}/messages`, {
      content: 'Work started',
      clientMessageId: uuid(),
    }).expect(201);
    await send(agents.pro, 'put', `/api/bookings/${main.id}/status`, {
      status: 'COMPLETED',
    }).expect(200);
    const view = (await agents.customer.get(`/api/conversations/${conversationId}`).expect(200))
      .body.conversation;
    assert.deepEqual([view.canSend, view.sendBlockedReason], [false, 'BOOKING_CLOSED']);
    await send(agents.customer, 'post', `/api/conversations/${conversationId}/messages`, {
      content: 'thanks',
      clientMessageId: uuid(),
    }).expect(403);
    assert.ok(
      (await agents.customer.get(`/api/conversations/${conversationId}/messages`).expect(200)).body
        .messages.length >= 4,
    );
  });

  await t.test(
    'master-key rotation re-wraps data keys and old messages stay readable',
    async () => {
      const oldSpec = process.env.CHAT_ENCRYPTION_KEYS;
      const newKey = `2:${randomBytes(32).toString('base64')}`;
      const env = {
        ...process.env,
        CHAT_ENCRYPTION_KEYS: `${oldSpec},${newKey}`,
        CHAT_ACTIVE_KEY_VERSION: '2',
      };
      const out = execFileSync('node', ['scripts/rotate-chat-keys.js'], { env, encoding: 'utf8' });
      assert.match(out, /version 2: \d+ conversation/);
      assert.equal(await db.conversation.count({ where: { keyVersion: 1 } }), 0);
      // A process that knows ONLY the new key can still decrypt the pre-rotation messages.
      const onlyNew = {
        ...process.env,
        CHAT_ENCRYPTION_KEYS: newKey,
        CHAT_ACTIVE_KEY_VERSION: '2',
      };
      const script = `
      import { db } from './src/config/db.js';
      import { unwrapConversationKey, decryptMessage } from './src/services/chatCrypto.js';
      const c = await db.conversation.findUnique({ where: { id: '${conversationId}' } });
      const m = await db.message.findUnique({ where: { id: '${first.id}' } });
      const key = unwrapConversationKey(c);
      console.log(decryptMessage(key, { conversationId: c.id, messageId: m.id, senderId: m.senderId }, m.encryptedContent));
      await db.$disconnect();`;
      const text = execFileSync('node', ['--input-type=module', '-e', script], {
        env: onlyNew,
        encoding: 'utf8',
      });
      assert.match(text, /are you coming today/);
      assert.equal(
        (await db.conversation.findUnique({ where: { id: conversationId } })).keyVersion,
        2,
      );
    },
  );

  await t.test('rate limits: message flooding is throttled per account', async () => {
    // A separate account, so the other tests' traffic does not share this limit window.
    const booking = await newBooking('03:00 PM - 04:00 PM', 'other');
    await assign(booking.id, proIds.pro2).expect(200);
    const convo = (
      await send(agents.other, 'post', '/api/conversations', { bookingId: booking.id }).expect(201)
    ).body.conversation;
    const statuses = [];
    for (let i = 0; i < 40; i += 1)
      statuses.push(
        (
          await send(agents.other, 'post', `/api/conversations/${convo.id}/messages`, {
            content: `flood ${i}`,
            clientMessageId: uuid(),
          })
        ).status,
      );
    assert.ok(statuses.includes(429), 'expected the send limiter to trigger');
    // 30 requests per minute count against this account; one was already spent probing a foreign thread.
    const accepted = statuses.filter((s) => s === 201).length;
    assert.ok(accepted >= 28 && accepted <= 30, `accepted ${accepted}`);
    assert.equal(statuses.at(-1), 429);
  });

  await t.test('message text never appeared in server logs', async () => {
    assert.deepEqual(
      logged.filter((line) => line.includes(MARKER)),
      [],
    );
    assert.deepEqual(
      logged.filter((line) => Object.values(phones).some((p) => line.includes(p))),
      [],
    );
  });
});
