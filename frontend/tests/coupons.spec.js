import { test, expect } from '@playwright/test';

// The cart's coupon flow in a real browser, from both names of the dev server (localhost and
// 127.0.0.1 are different origins to the API). No login is needed: the coupon preview is public and
// only the server prices the cart.
const configured = process.env.E2E_URL || 'http://localhost:5173';
const bases = [...new Set([configured, configured.replace('//localhost', '//127.0.0.1')])];

for (const base of bases) {
  test(`cart coupons work and are priced by the server at ${base}`, async ({ browser }) => {
    test.setTimeout(120000);
    const context = await browser.newContext({ baseURL: base });
    const page = await context.newPage();
    const pageErrors = [];
    const validations = [];
    page.on('pageerror', (e) => pageErrors.push(e.message));
    page.on('response', async (response) => {
      if (!response.url().includes('/api/coupons/validate')) return;
      validations.push({
        status: response.status(),
        request: response.request().postDataJSON(),
        body: await response.json().catch(() => null),
      });
    });
    await page.route('https://**/*', (route) => route.abort()); // remote imagery/fonts are out of scope

    const find = async (search, category) => {
      const { services } = await (await page.request.get(`/api/services?search=${search}&limit=50`)).json();
      return services.find((s) => s.categoryId === category);
    };
    const lighting = await find('lighting', 'cat-home-improvement');
    const cleaning = await find('cleaning', 'cat-cleaning');
    expect(lighting && cleaning).toBeTruthy();

    const money = async (label) =>
      Number(
        (await page.locator('div.flex.justify-between', { hasText: label }).last().locator('span').last().textContent()).replace(/[^\d.]/g, ''),
      );
    const addToCart = async (service) => {
      await page.goto(`/services/${service.slug}`, { waitUntil: 'domcontentloaded' });
      await page.getByRole('button', { name: /Add to Cart/ }).first().click();
      await expect(page.getByRole('heading', { name: 'Your Service Cart' })).toBeVisible();
    };

    // The reported scenario: the lighting service in the cart, WELCOME150 applied.
    await addToCart(lighting);
    const itemTotal = await money('Item Total');
    await page.getByRole('button', { name: /WELCOME150/ }).click();
    await expect(page.getByText('Coupon Discount')).toBeVisible();
    expect(await money('Coupon Discount')).toBe(150);
    expect(await money('Total Amount')).toBe(itemTotal - 150);
    const welcome = validations.at(-1);
    expect(welcome.status).toBe(200);
    // The browser sends only the code and which items are in the cart, never an amount or discount.
    expect(Object.keys(welcome.request).sort()).toEqual(['code', 'items']);
    expect(Object.keys(welcome.request.items[0]).sort()).toEqual(['quantity', 'serviceId', 'variantId']);
    expect(welcome.body.subtotal).toBe(itemTotal); // and the server priced the same cart itself
    await expect(page.getByText('Origin is not allowed')).toHaveCount(0);

    // CLEAN10 is for cleaning and pest control; the lighting cart is refused with the real reason.
    await page.getByRole('button', { name: 'Remove', exact: true }).click();
    await page.getByRole('button', { name: /CLEAN10/ }).click();
    await expect(page.getByText(/CLEAN10 applies only to .*Home Cleaning/)).toBeVisible();
    await expect(page.getByText('Coupon Discount')).toHaveCount(0);
    expect(validations.at(-1).status).toBe(400);

    // An invalid code shows the server's message and changes nothing.
    await page.getByPlaceholder(/Enter coupon code/).fill('NOTACODE');
    await page.getByRole('button', { name: 'Apply', exact: true }).click();
    await expect(page.getByText('This coupon code is not valid')).toBeVisible();
    expect(await money('Total Amount')).toBe(itemTotal);

    // With a cleaning service in the cart CLEAN10 applies, to the eligible item only, as the server says.
    await addToCart(cleaning);
    await page.getByRole('button', { name: /CLEAN10/ }).click();
    await expect(page.getByText('Coupon Discount')).toBeVisible();
    const clean = validations.at(-1);
    expect(clean.status).toBe(200);
    expect(clean.body.discount).toBeGreaterThan(0);
    expect(clean.body.discount).toBeLessThanOrEqual(200);
    expect(await money('Coupon Discount')).toBe(clean.body.discount);
    expect(await money('Total Amount')).toBe(clean.body.total);
    expect(clean.body.discount).toBeLessThan(clean.body.subtotal * 0.1 + 1); // never 10% of the whole cart

    // Removing the cleaning service re-prices the coupon: it no longer fits, so it is dropped with a reason.
    await page.getByRole('button', { name: `Remove ${cleaning.name}` }).click();
    await expect(page.getByText(/CLEAN10 applies only to .*Home Cleaning/)).toBeVisible();
    await expect(page.getByText('Coupon Discount')).toHaveCount(0);
    expect(await money('Total Amount')).toBe(itemTotal);
    expect(pageErrors).toEqual([]);
    expect(validations.some((v) => v.status === 403)).toBe(false); // no "Origin is not allowed"
    await context.close();
  });
}
