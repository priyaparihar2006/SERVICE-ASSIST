import { test, expect } from '@playwright/test';

const path = '/services/ro-water-purifier-service';
const images = [
  'servicing',
  'inspection water test',
  'filter replacement',
  'installation',
  'maintenance cartridges',
];

test('RO gallery loads on direct navigation, survives refresh and keeps booking actions working', async ({ page }) => {
  test.setTimeout(120000);
  await page.goto(path, { waitUntil: 'domcontentloaded' });
  await expect(page.getByRole('heading', { name: 'RO Water Purifier Service & Filter Replacement' })).toBeVisible();
  const gallery = page.getByLabel('Service image gallery');
  await expect(gallery.getByRole('button')).toHaveCount(5);

  for (const label of images) {
    await gallery.getByRole('button', { name: `View ${label} image` }).click();
    const main = page.locator('main img[alt*="RO Water Purifier Service"]').first();
    await expect.poll(() => main.evaluate((image) => image.complete && image.naturalWidth > 0)).toBe(true);
    await expect(main).toHaveAttribute('src', new RegExp(label.replaceAll(' ', '-') + '\\.png$'));
  }

  for (const width of [1440, 768, 390]) {
    await page.setViewportSize({ width, height: 900 });
    const image = page.locator('main img[alt*="RO Water Purifier Service"]').first();
    const box = await image.boundingBox();
    expect(box).not.toBeNull();
    expect(box.x).toBeGreaterThanOrEqual(0);
    expect(box.x + box.width).toBeLessThanOrEqual(width + 1);
  }

  await page.reload({ waitUntil: 'domcontentloaded' });
  await expect(page.getByRole('heading', { name: 'RO Water Purifier Service & Filter Replacement' })).toBeVisible();
  await expect.poll(() => page.locator('main img[alt*="RO Water Purifier Service"]').first().evaluate((image) => image.complete && image.naturalWidth > 0)).toBe(true);
  await page.getByRole('button', { name: 'Add to Cart & Select Slot' }).click();
  await expect(page.getByText('Your Service Cart')).toBeVisible();
  await page.getByRole('button', { name: 'Close cart' }).click();
  await page.getByRole('button', { name: 'Book Instant Now' }).click();
  await expect(page.getByText('Your Service Cart')).toBeVisible();
});

test('RO gallery keeps a visible fallback when one photo fails', async ({ page }) => {
  await page.route('**/service-images/ro-purifier/installation.png', (route) => route.abort());
  await page.goto(path, { waitUntil: 'domcontentloaded' });
  await page.getByRole('button', { name: 'View installation image' }).click();
  await expect(page.getByText('RO Water Purifier Service & Filter Replacement').first()).toBeVisible();
  await expect(page.getByRole('status', { name: 'Loading service image' })).toHaveCount(0);
  await page.getByRole('button', { name: 'View servicing image' }).click();
  await expect.poll(() => page.locator('main img[alt*="RO Water Purifier Service"]').first().evaluate((image) => image.complete && image.naturalWidth > 0)).toBe(true);
});
