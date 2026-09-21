import { test, expect } from '@playwright/test';

test('professional registration and business availability settings', async ({ page }) => {
  const errors = [];
  page.on('pageerror', error => errors.push(error.message));
  await page.route('https://**/*', route => route.abort());
  await page.goto('/professional/dashboard', { waitUntil: 'domcontentloaded' });
  await page.getByRole('button', { name: 'Sign in or register' }).click();
  await page.getByRole('button', { name: 'Create New Account' }).click();
  await page.getByPlaceholder('e.g. Priya Sharma').fill('Browser Professional');
  await page.getByPlaceholder('name@example.com').fill(`browser-pro-${Date.now()}@demo.service-assist.test`);
  await page.getByLabel('Password', { exact: true }).fill(`Professional-${Date.now()}`);
  await page.locator('form').getByRole('button', { name: 'professional', exact: true }).click();
  await page.getByRole('button', { name: 'Create Account', exact: true }).click();
  await expect(page.getByText('No assigned bookings yet.')).toBeVisible();
  await page.getByText('Business profile, services & availability', { exact: true }).click();
  await page.getByLabel('Business name').fill('Browser Repair Team');
  await page.getByLabel('Cities (comma separated)').fill('Agra');
  await page.getByLabel('Foam Jet Power AC Servicing', { exact: true }).check();
  await page.getByLabel('Mon', { exact: true }).check();
  await page.getByLabel('Accept assignments within these hours').check();
  await page.getByRole('button', { name: 'Save business settings' }).click();
  await expect(page.getByText('Saved. Service changes require administrator verification.')).toBeVisible();
  await page.reload({ waitUntil: 'domcontentloaded' });
  await expect(page.getByText('Browser Repair Team', { exact: false }).first()).toBeVisible();
  expect(errors).toEqual([]);
});

test('administrator metrics and management load with a real admin session', async ({ page }) => {
  test.skip(!process.env.E2E_ADMIN_EMAIL, 'Run backend npm run test:browser to supply temporary admin credentials');
  const errors = [];
  page.on('pageerror', error => errors.push(error.message));
  await page.route('https://**/*', route => route.abort());
  await page.goto('/admin', { waitUntil: 'domcontentloaded' });
  await page.getByRole('button', { name: 'Sign in or register' }).click();
  await page.getByPlaceholder('name@example.com').fill(process.env.E2E_ADMIN_EMAIL);
  await page.getByLabel('Password', { exact: true }).fill(process.env.E2E_ADMIN_PASSWORD);
  await page.locator('form').getByRole('button', { name: 'Sign In', exact: true }).click();
  await expect(page.getByText('Service Assist Admin Operations')).toBeVisible();
  await page.getByText('Manage professionals, categories & services', { exact: true }).click();
  await expect(page.getByText('Professional verification', { exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Save category', exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Save service', exact: true })).toBeVisible();
  expect(errors).toEqual([]);
});
