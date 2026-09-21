import { test, expect } from '@playwright/test';

test('catalog, authentication, booking, persistence and customer access control', async ({
  page,
}) => {
  test.setTimeout(120000);
  const errors = [];
  page.on('pageerror', (e) => errors.push(e.message));
  page.on('dialog', (dialog) => dialog.dismiss());
  // Remote promotional imagery is not part of API integration testing.
  await page.route('https://**/*', (route) => route.abort());
  await page.goto('/services?category=cat-sofa-cleaning', {
    waitUntil: 'domcontentloaded',
  });
  await expect(
    page
      .getByText('Sofa & Upholstery Deep Shampooing', { exact: false })
      .first(),
  ).toBeVisible();
  const categories = await (await page.request.get('/api/categories')).json();
  for (const category of categories.categories) {
    await page
      .locator('#service-catalog-results')
      .getByRole('button', { name: category.name, exact: true })
      .click();
    await expect(page.locator('#service-catalog-results')).toBeVisible();
    await expect(
      page.getByText(/Showing [1-9]\d* verified services/),
    ).toBeVisible();
  }
  await page.goto('/admin', { waitUntil: 'domcontentloaded' });
  await expect(page.getByText('Please sign in to continue.')).toBeVisible();
  await page
    .getByRole('button', { name: 'Sign in or register', exact: true })
    .click();
  await page.getByRole('button', { name: 'Create New Account' }).click();
  const email = `browser-${Date.now()}@demo.service-assist.test`;
  const password = `Browser-test-${Date.now()}`;
  await page
    .getByPlaceholder('e.g. Priya Sharma')
    .fill('Browser Test Customer');
  await page.getByPlaceholder('name@example.com').fill(email);
  await page.getByLabel('Password', { exact: true }).fill(password);
  await page
    .getByRole('button', { name: 'Create Account', exact: true })
    .click();
  await expect(
    page.getByText('This page requires a admin account.'),
  ).toBeVisible();
  await page.goto('/services/ac-jet-service', {
    waitUntil: 'domcontentloaded',
  });
  await page.getByRole('button', { name: /^Add/ }).first().click();
  await page
    .getByRole('button', { name: 'Proceed to Schedule & Checkout' })
    .click();
  await expect(
    page.getByText('Schedule & Checkout', { exact: true }),
  ).toBeVisible();
  await page.getByRole('button', { name: 'Continue', exact: true }).click();
  await page
    .getByRole('button', { name: 'Add Address Now', exact: true })
    .click();
  await page.getByPlaceholder('House / Flat / Block No.').fill('12 Test House');
  await page.getByPlaceholder('Street / Road / Colony').fill('Test Road');
  await page.getByPlaceholder('Area / Landmark').fill('Test Area');
  await page
    .getByRole('button', { name: 'Save & Select', exact: true })
    .click();
  await expect(
    page.getByText('12 Test House, Test Road, Test Area'),
  ).toBeVisible();
  await page.getByRole('button', { name: 'Continue', exact: true }).click();
  const bookingResponse = page.waitForResponse(
    (r) => r.url().endsWith('/api/bookings') && r.request().method() === 'POST',
  );
  await page.getByRole('button', { name: /Confirm & Book for/ }).click();
  const response = await bookingResponse;
  expect(response.status()).toBe(201);
  const { booking } = await response.json();
  await expect(page.getByText("We've Received Your Booking")).toBeVisible();
  await page.getByRole('button', { name: 'Done & View My Bookings' }).click();
  await page.reload();
  await expect(
    page.getByText(booking.id, { exact: false }).first(),
  ).toBeVisible();
  await expect(page.getByText('Waiting for professional assignment').first()).toBeVisible();
  await expect(page.getByRole('button', { name: 'Chat with Professional' })).toBeDisabled();
  expect(booking.paymentStatus).toBe('PENDING');
  expect(errors).toEqual([]);
});

test('wrong password and unavailable backend show errors', async ({ page }) => {
  await page.route('https://**/*', (route) => route.abort());
  await page.goto('/dashboard', { waitUntil: 'domcontentloaded' });
  await page
    .getByRole('button', { name: 'Sign in or register', exact: true })
    .click();
  await page.getByPlaceholder('name@example.com').fill('unknown@example.test');
  await page
    .getByLabel('Password', { exact: true })
    .fill('Not-a-real-password');
  await page
    .locator('form')
    .getByRole('button', { name: 'Sign In', exact: true })
    .click();
  await expect(page.getByRole('alert')).toContainText(
    'Email or password is incorrect',
  );
  await page.route('**/api/categories', (route) => route.abort());
  await page.reload();
  await expect(page.getByRole('alert')).toContainText(
    'Unable to reach Service Assist',
  );
});
