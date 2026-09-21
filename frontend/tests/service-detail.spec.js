import { test, expect } from '@playwright/test';
test('service detail renders', async ({ page }) => {
  const errors = [];
  page.on('pageerror', error => { errors.push(error.message); console.log('Browser exception:', error.message); });
  await page.route('https://**/*', route => route.abort());
  await page.goto('/services/ac-jet-service', { waitUntil: 'domcontentloaded' });
  await expect(page.getByRole('button', { name: 'Add to Cart & Select Slot' })).toBeVisible({ timeout: 10000 });
  expect(errors).toEqual([]);
});
