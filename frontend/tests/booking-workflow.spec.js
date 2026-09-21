import { test, expect } from '@playwright/test';

test('admin dispatch, professional response and customer status update without reload', async ({ browser }) => {
  test.skip(!process.env.E2E_ADMIN_EMAIL, 'Run through backend npm run test:browser');
  test.setTimeout(120000);
  const baseURL = process.env.E2E_URL || 'http://localhost:5173';
  const origin = new URL(baseURL).origin;
  const stamp = `${Date.now()}-${Math.random().toString(36).slice(2)}`;
  const password = `Workflow-${stamp}-Password`;
  const customerEmail = `workflow-customer-${stamp}@demo.service-assist.test`;
  const proEmail = `workflow-pro-${stamp}@demo.service-assist.test`;
  const contexts = await Promise.all([0, 1, 2].map(() => browser.newContext({ baseURL })));
  const [customerContext, proContext, adminContext] = contexts;
  const [customerPage, proPage, adminPage] = await Promise.all(contexts.map((c) => c.newPage()));
  for (const page of [customerPage, proPage, adminPage])
    await page.route('https://**/*', (route) => route.abort());
  const call = async (context, method, path, data, headers = {}) => {
    const response = await context.request[method](`/api${path}`, {
      data, headers: { Origin: origin, ...headers },
    });
    expect(response.ok(), `${method.toUpperCase()} ${path}: ${await response.text()}`).toBeTruthy();
    return response.json();
  };
  try {
    await call(customerContext, 'post', '/auth/register', { name: 'Workflow Customer', email: customerEmail, password });
    await call(proContext, 'post', '/professionals/register', { name: 'Workflow Professional', email: proEmail, password, businessName: 'Workflow Repairs' });
    await call(adminContext, 'post', '/auth/login', { email: process.env.E2E_ADMIN_EMAIL, password: process.env.E2E_ADMIN_PASSWORD });
    const { services } = await call(customerContext, 'get', '/services?limit=1');
    const service = services[0];
    const { professional } = await call(proContext, 'get', '/professionals/profile');
    await call(proContext, 'put', '/professionals/profile', {
      businessName: 'Workflow Repairs', serviceIds: [service.id], serviceArea: ['Agra'], isAvailableToday: true,
    });
    await call(proContext, 'put', '/professionals/availability', {
      availability: Array.from({ length: 7 }, (_, dayOfWeek) => ({ dayOfWeek, startMinute: 480, endMinute: 1140 })),
    });
    await call(adminContext, 'put', `/admin/professionals/${professional.id}/verification`, { verificationStatus: 'VERIFIED' });
    const { address } = await call(customerContext, 'post', '/addresses', {
      house: '12', street: 'Workflow Street', city: 'Agra', state: 'Uttar Pradesh', pincode: '282001', isDefault: true,
    });
    const date = new Date(Date.now() + 7 * 86400000).toISOString().slice(0, 10);
    const { booking } = await call(customerContext, 'post', '/bookings', {
      addressId: address.id, items: [{ serviceId: service.id, variantId: service.variants[0].id, quantity: 1 }],
      bookingDate: date, bookingTime: '02:00 PM - 03:00 PM', paymentMethod: 'CASH',
    }, { 'Idempotency-Key': crypto.randomUUID() });

    await customerPage.goto('/dashboard');
    await proPage.goto('/professional/dashboard');
    await adminPage.goto('/admin');
    await expect(customerPage.getByText('Waiting for professional assignment').first()).toBeVisible();
    await expect(customerPage.getByRole('button', { name: 'Chat with Professional' })).toBeDisabled();
    const row = adminPage.getByRole('row').filter({ hasText: booking.id });
    await row.getByRole('button', { name: 'Find eligible professionals' }).click();
    await row.getByRole('combobox', { name: `Assign ${booking.id}` }).selectOption(professional.id);
    await expect(proPage.getByRole('button', { name: 'Accept booking request' })).toBeVisible();
    await expect(customerPage.getByText('Workflow Professional', { exact: true })).toBeVisible();
    await expect(customerPage.getByRole('button', { name: 'Chat with Professional' })).toBeEnabled();
    await proPage.getByRole('button', { name: 'Accept booking request' }).click();
    await expect(customerPage.getByText('Expert Accepted')).toBeVisible();
    expect(await customerPage.content()).not.toContain(proEmail);
    expect(await proPage.content()).not.toContain(customerEmail);
  } finally {
    await Promise.all(contexts.map((c) => c.close()));
  }
});
