import { test, expect } from '@playwright/test';

test('expanded service catalog supports browsing, local images, search, details and cart on mobile', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto('/services?category=cat-laptop-computer');
  await expect(page.getByRole('heading', { name: 'All Doorstep Services' })).toBeVisible();
  await expect(page.getByLabel('Available in Agra')).toBeChecked();
  await page.getByRole('button', { name: 'Laptop Repair', exact: true }).click();
  await page.getByPlaceholder(/Search services/).fill('  laptop screen replacement  ');
  const card = page.locator('.group').filter({ hasText: 'Laptop Screen Replacement' }).first();
  await expect(card).toBeVisible();
  await expect(card.getByText('Inspection fee')).toBeVisible();
  const image = card.locator('img');
  await expect(image).toBeVisible();
  await expect.poll(() => image.evaluate((el) => el.complete && el.naturalWidth > 0)).toBe(true);
  await card.getByRole('button', { name: 'Details' }).click();
  await expect(page.getByRole('heading', { name: 'Laptop Screen Replacement' })).toBeVisible();
  await expect(page.getByText(/listed amount is an inspection fee/i)).toBeVisible();
  await page.getByRole('button', { name: /^Add/ }).first().click();
  await expect(page.getByText('Laptop Screen Replacement').first()).toBeVisible();
});
