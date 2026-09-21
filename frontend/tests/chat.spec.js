import { test, expect, request as playwrightRequest } from '@playwright/test';

// Real browsers, real API, real WebSocket: separate customer, professional, outsider and admin
// accounts. Needs the admin fixture from `npm run test:browser` in backend/.
const baseURL = process.env.E2E_URL || 'http://localhost:5173';
const origin = new URL(baseURL).origin;
const stamp = Date.now();
const password = `Chat-e2e-${stamp}-Pw`;
const accounts = {
  customer: { name: 'Riya Sharma', email: `chat-customer-${stamp}@demo.service-assist.test`, phone: '9811100001' },
  pro: { name: 'Manoj Plumber', email: `chat-pro-${stamp}@demo.service-assist.test`, phone: '9811100002' },
  outsider: { name: 'Nosy Neighbour', email: `chat-out-${stamp}@demo.service-assist.test`, phone: '9811100003' },
};
let bookingId;

async function api(ctx, method, path, data, headers = {}) {
  const response = await ctx[method](`/api${path}`, { data, headers: { Origin: origin, ...headers } });
  if (!response.ok()) throw new Error(`${method.toUpperCase()} ${path} -> ${response.status()} ${await response.text()}`);
  return response.json();
}

async function signedIn(browser, account, viewport) {
  const context = await browser.newContext({ baseURL, viewport });
  const page = await context.newPage();
  await page.route('https://**/*', (route) => route.abort()); // remote imagery/fonts are out of scope
  await api(page.request, 'post', '/auth/login', { email: account.email, password: password });
  return { context, page };
}

test.describe.configure({ mode: 'serial' });

test.beforeAll(async () => {
  test.skip(!process.env.E2E_ADMIN_EMAIL, 'Run via backend `npm run test:browser` to supply admin credentials');
  const anon = await playwrightRequest.newContext({ baseURL });
  const customer = await playwrightRequest.newContext({ baseURL });
  const pro = await playwrightRequest.newContext({ baseURL });
  const admin = await playwrightRequest.newContext({ baseURL });
  await api(customer, 'post', '/auth/register', { ...accounts.customer, password });
  await api(anon, 'post', '/auth/register', { ...accounts.outsider, password });
  await api(pro, 'post', '/professionals/register', { ...accounts.pro, password, businessName: 'Manoj Plumbing' });
  await api(admin, 'post', '/auth/login', { email: process.env.E2E_ADMIN_EMAIL, password: process.env.E2E_ADMIN_PASSWORD });
  const { services } = await api(customer, 'get', '/services?limit=1');
  const service = services[0];
  const professional = (await api(pro, 'get', '/professionals/profile')).professional;
  await api(pro, 'put', '/professionals/profile', {
    businessName: 'Manoj Plumbing',
    serviceIds: [service.id],
    serviceArea: ['Agra'],
    isAvailableToday: true,
  });
  await api(pro, 'put', '/professionals/availability', {
    availability: Array.from({ length: 7 }, (_, dayOfWeek) => ({ dayOfWeek, startMinute: 480, endMinute: 1140 })),
  });
  await api(admin, 'put', `/admin/professionals/${professional.id}/verification`, { verificationStatus: 'VERIFIED' });
  const { address } = await api(customer, 'post', '/addresses', {
    house: '12', street: 'Browser Lane', city: 'Agra', state: 'Uttar Pradesh', pincode: '282001', isDefault: true,
  });
  const date = new Date(Date.now() + 5 * 86400000).toISOString().slice(0, 10);
  const { booking } = await api(
    customer,
    'post',
    '/bookings',
    { addressId: address.id, items: [{ serviceId: service.id, variantId: service.variants[0].id, quantity: 1 }], bookingDate: date, bookingTime: '10:00 AM - 11:00 AM', paymentMethod: 'CASH' },
    { 'Idempotency-Key': crypto.randomUUID() },
  );
  bookingId = booking.id;
  await api(admin, 'put', `/bookings/${bookingId}/status`, { status: 'ASSIGNED', professionalId: professional.id });
  await api(pro, 'put', `/professionals/bookings/${bookingId}/status`, { status: 'CONFIRMED' });
  await Promise.all([anon, customer, pro, admin].map((c) => c.dispose()));
});

test('customer and professional chat privately in real time, on desktop', async ({ browser }, info) => {
  test.setTimeout(120000);
  const errors = [];
  const { page: customer, context: customerContext } = await signedIn(browser, accounts.customer, { width: 1360, height: 900 });
  const { page: pro, context: proContext } = await signedIn(browser, accounts.pro, { width: 1360, height: 900 });
  for (const page of [customer, pro]) page.on('pageerror', (e) => errors.push(e.message));

  // The customer opens the booking and starts the chat from it.
  await customer.goto('/dashboard', { waitUntil: 'domcontentloaded' });
  await expect(customer.getByText(bookingId).first()).toBeVisible();
  await expect(customer.getByRole('button', { name: 'Call Partner' })).toHaveCount(0);
  expect(await customer.content()).not.toContain(accounts.pro.phone);
  await customer.getByRole('button', { name: 'Chat with Professional' }).click();
  await expect(customer.getByTestId('chat-title')).toHaveText('Manoj Plumber');
  await expect(customer.getByTestId('chat-window')).toContainText(`Booking ${bookingId}`);
  await expect(customer.getByTestId('chat-window')).toContainText(/\S+/);
  await expect(customer.getByTestId('chat-empty-thread')).toBeVisible();
  await expect(customer.getByTestId('chat-connection')).toHaveText('Live');
  await expect(customer).toHaveURL(/\/messages\?c=/);
  await customer.screenshot({ path: `test-results/chat-desktop-empty.png` });

  // The professional sees the conversation in their inbox.
  await pro.goto('/messages', { waitUntil: 'domcontentloaded' });
  await expect(pro.getByTestId('chat-list-item')).toHaveCount(1);
  await expect(pro.getByTestId('chat-list-item')).toContainText('Riya Sharma');
  await pro.getByTestId('chat-list-item').click();
  await expect(pro.getByTestId('chat-title')).toHaveText('Riya Sharma');
  await expect(customer.getByTestId('chat-presence')).toHaveText('Online'); // presence, live

  // Customer -> professional, delivered instantly and marked read because the thread is open.
  const box = customer.getByRole('textbox', { name: 'Message' });
  await box.fill('Hello, will you reach by 10:30? The gate code is at the guard desk.');
  await box.press('Enter');
  const mine = customer.getByTestId('chat-message').filter({ hasText: 'will you reach by 10:30' });
  await expect(mine).toBeVisible();
  await expect(pro.getByTestId('chat-message').filter({ hasText: 'will you reach by 10:30' })).toBeVisible();
  await expect(mine.getByRole('img', { name: 'Read' })).toBeVisible();

  // Typing indicator, then the reply arrives in the customer's open window.
  await pro.getByRole('textbox', { name: 'Message' }).pressSequentially('On my way', { delay: 30 });
  await expect(customer.getByTestId('chat-typing')).toBeVisible();
  await expect(customer.getByTestId('chat-presence')).toHaveText('typing…');
  await pro.getByRole('textbox', { name: 'Message' }).press('Enter');
  await expect(customer.getByTestId('chat-message').filter({ hasText: 'On my way' })).toBeVisible();
  await expect(customer.getByTestId('chat-typing')).toHaveCount(0);

  // Sharing contact details is refused, with a way to fix the message.
  await box.fill('Please call me on 98765 43210');
  await box.press('Enter');
  await expect(customer.getByRole('alert').filter({ hasText: 'cannot be shared in chat' })).toBeVisible();
  await expect(pro.getByText('98765')).toHaveCount(0);
  await customer.getByRole('button', { name: 'Edit', exact: true }).click();
  await expect(box).toHaveValue('Please call me on 98765 43210');
  await box.fill('');

  // Honest privacy explanation.
  await customer.getByRole('button', { name: /Private chat/ }).click();
  await expect(customer.locator('#chat-privacy-panel')).toContainText('not end-to-end encrypted');
  await expect(customer.locator('#chat-privacy-panel')).toContainText('never shown');
  await customer.screenshot({ path: `test-results/chat-desktop-conversation.png` });
  await customer.getByRole('button', { name: 'Close privacy information' }).click();

  // Markup in a message is shown as plain text and never runs.
  const payload = '<img src=x onerror="window.__xss=1"><script>window.__xss=2</script>';
  await box.fill(payload);
  await box.press('Enter');
  const hostile = customer.getByTestId('chat-message').filter({ hasText: '<script>window.__xss=2</script>' });
  await expect(hostile).toBeVisible();
  await expect(pro.getByTestId('chat-message').filter({ hasText: '<script>window.__xss=2</script>' })).toBeVisible();
  for (const page of [customer, pro]) {
    expect(await page.locator('[data-testid=chat-message] img, [data-testid=chat-message] script').count()).toBe(0);
    expect(await page.evaluate(() => window.__xss)).toBeUndefined();
  }

  // Deleting your own message removes it for both sides.
  await customer.getByTestId('chat-message').filter({ hasText: 'will you reach by 10:30' }).hover();
  await customer.getByRole('button', { name: 'Delete message' }).first().click();
  await customer.getByRole('button', { name: 'Delete', exact: true }).click();
  await expect(pro.getByText('This message was deleted')).toBeVisible();

  // Nobody sees anyone's number, and nothing is kept in browser storage.
  expect(await pro.content()).not.toContain(accounts.customer.phone);
  expect(await customer.content()).not.toContain(accounts.pro.phone);
  const stored = await customer.evaluate(() => JSON.stringify({ ...localStorage }) + JSON.stringify({ ...sessionStorage }));
  expect(stored).not.toContain('gate code');
  expect(stored).not.toContain('On my way');

  // The professional's dashboard offers chat, not a phone call.
  await pro.goto('/professional/dashboard', { waitUntil: 'domcontentloaded' });
  await expect(pro.getByText('Contact through in-app chat').first()).toBeVisible();
  await expect(pro.getByRole('button', { name: 'Call Customer' })).toHaveCount(0);
  expect(await pro.content()).not.toContain(accounts.customer.phone);
  await pro.getByRole('button', { name: 'Chat with Customer' }).click();
  await expect(pro.getByTestId('chat-title')).toHaveText('Riya Sharma');

  // Secure logout: chat state is gone and the socket is closed.
  await customer.getByRole('button', { name: /Riya Sharma/ }).first().click();
  await customer.getByRole('button', { name: 'Sign Out' }).click();
  await customer.goto('/messages', { waitUntil: 'domcontentloaded' });
  await expect(customer.getByText('Please sign in to view your messages.')).toBeVisible();
  expect(await customer.content()).not.toContain('gate code');
  await expect(pro.getByTestId('chat-presence')).not.toHaveText('Online', { timeout: 15000 });

  expect(errors).toEqual([]);
  await customerContext.close();
  await proContext.close();
});

test('an unrelated user cannot open the conversation, even with its id', async ({ browser }) => {
  const { page: pro, context: proContext } = await signedIn(browser, accounts.pro, { width: 1200, height: 800 });
  await pro.goto('/messages', { waitUntil: 'domcontentloaded' });
  const conversationId = await pro.getByTestId('chat-list-item').first().click().then(() => new URL(pro.url()).searchParams.get('c'));
  expect(conversationId).toBeTruthy();
  const { page: outsider, context: outsiderContext } = await signedIn(browser, accounts.outsider, { width: 1200, height: 800 });
  await outsider.goto(`/messages?c=${conversationId}`, { waitUntil: 'domcontentloaded' });
  await expect(outsider.getByTestId('chat-error')).toBeVisible();
  await expect(outsider.getByTestId('chat-empty')).toBeVisible();
  expect(await outsider.content()).not.toContain('Manoj');
  await outsider.goto(`/messages?booking=${bookingId}`, { waitUntil: 'domcontentloaded' });
  await expect(outsider.getByTestId('chat-open-error')).toBeVisible();
  await proContext.close();
  await outsiderContext.close();
});

test('mobile layout: list, full-screen conversation, no horizontal scrolling', async ({ browser }) => {
  test.setTimeout(90000);
  const { page, context } = await signedIn(browser, accounts.customer, { width: 375, height: 740 });
  const overflow = () => page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
  await page.goto('/messages', { waitUntil: 'domcontentloaded' });
  await expect(page.getByTestId('chat-list-item')).toHaveCount(1);
  await expect(page.getByTestId('chat-window')).toHaveCount(0); // list only on small screens
  expect(await overflow()).toBeLessThanOrEqual(0);
  await page.screenshot({ path: 'test-results/chat-mobile-list.png' });
  await page.getByTestId('chat-list-item').click();
  await expect(page.getByTestId('chat-window')).toBeVisible();
  await expect(page.getByTestId('chat-list-item')).toBeHidden();
  const box = page.getByRole('textbox', { name: 'Message' });
  await expect(box).toBeVisible();
  const size = await box.evaluate((el) => parseFloat(getComputedStyle(el).fontSize));
  expect(size).toBeGreaterThanOrEqual(16); // avoids the iOS focus-zoom
  await box.fill('Sent from my phone, thanks!');
  await page.getByRole('button', { name: 'Send message' }).click();
  await expect(page.getByTestId('chat-message').filter({ hasText: 'Sent from my phone' })).toBeVisible();
  expect(await overflow()).toBeLessThanOrEqual(0);
  await page.screenshot({ path: 'test-results/chat-mobile-conversation.png' });
  await page.getByRole('button', { name: 'Back to conversations' }).click();
  await expect(page.getByTestId('chat-list-item')).toBeVisible();

  // Network loss: the failed send is explained and can be retried safely.
  await page.getByTestId('chat-list-item').click();
  await context.setOffline(true);
  await box.fill('Sent while offline');
  await box.press('Enter');
  await expect(page.getByRole('alert').filter({ hasText: /Unable to reach/ })).toBeVisible();
  await context.setOffline(false);
  await page.getByRole('button', { name: 'Retry' }).click();
  await expect(page.getByTestId('chat-message').filter({ hasText: 'Sent while offline' }).getByRole('img', { name: /Sent|Delivered|Read/ })).toBeVisible();
  await expect(page.getByTestId('chat-message').filter({ hasText: 'Sent while offline' })).toHaveCount(1); // no duplicate
  await context.close();
});
