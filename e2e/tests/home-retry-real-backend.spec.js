// @ts-check
const { test, expect } = require('../fixtures/fixtures');

test.describe('Home category retry — real seeded backend', () => {
  test.use({ viewport: { width: 1280, height: 844 } });

  test('retries categories against the seeded backend after an initial failure', async ({ page, homePage }) => {
    const isCategoryEndpoint = (url) => new URL(url).pathname === '/api/categories';
    await page.route(isCategoryEndpoint, async (route) => {
      await route.fulfill({ status: 500, contentType: 'application/json', body: '{"error":"boom"}' });
    });

    await homePage.open('/');

    const alert = page.locator('.categories-alert[role="alert"]');
    await expect(alert).toBeVisible();
    await expect(alert.getByText('No pudimos cargar las categorías.')).toBeVisible();

    await page.unroute(isCategoryEndpoint);
    const categoryResponse = page.waitForResponse((response) =>
      new URL(response.url()).pathname === '/api/categories' && response.request().method() === 'GET',
    );
    await alert.getByRole('button', { name: 'Reintentar' }).click();

    const categoryRetryResponse = await categoryResponse;
    expect(categoryRetryResponse.status()).toBe(200);
    const categories = await categoryRetryResponse.json();
    expect(Array.isArray(categories)).toBe(true);
    expect(categories.length).toBeGreaterThan(0);
    expect(typeof categories[0]?.name).toBe('string');
    expect(categories[0].name).not.toBe('');

    await expect(page.locator('.category-tag .category-name', { hasText: categories[0].name }).first()).toBeVisible();
    await expect(alert).toHaveCount(0);
  });
});

test.describe('Home search retry — real seeded backend', () => {
  test.use({ viewport: { width: 1280, height: 844 } });

  test('retries a Buenos Aires search against the seeded backend after an initial failure', async ({ page, homePage }) => {
    const isBuenosAiresSearchEndpoint = (url) => {
      const requestUrl = new URL(url);
      return requestUrl.pathname === '/api/lodgings/search' && requestUrl.searchParams.get('city') === 'Buenos Aires';
    };
    await page.route(isBuenosAiresSearchEndpoint, async (route) => {
      await route.fulfill({ status: 500, contentType: 'application/json', body: '{"error":"boom"}' });
    });

    await homePage.open('/?city=Buenos%20Aires');

    const alert = page.locator('.search-results-alert[role="alert"]');
    await expect(alert).toBeVisible();
    await expect(alert.getByText('No pudimos realizar la búsqueda.')).toBeVisible();

    await page.unroute(isBuenosAiresSearchEndpoint);
    const searchResponse = page.waitForResponse((response) => {
      const responseUrl = new URL(response.url());
      return (
        responseUrl.pathname === '/api/lodgings/search' &&
        responseUrl.searchParams.get('city') === 'Buenos Aires' &&
        response.request().method() === 'GET'
      );
    });
    await alert.getByRole('button', { name: 'Reintentar' }).click();

    const searchRetryResponse = await searchResponse;
    expect(searchRetryResponse.status()).toBe(200);
    const searchResults = await searchRetryResponse.json();
    expect(searchResults.totalItems).toBeGreaterThan(0);
    expect(Array.isArray(searchResults.lodgings)).toBe(true);
    expect(searchResults.lodgings.length).toBeGreaterThan(0);
    expect(searchResults.lodgings.every((lodging) => lodging.city === 'Buenos Aires')).toBe(true);
    expect(typeof searchResults.lodgings[0]?.name).toBe('string');
    expect(searchResults.lodgings[0].name).not.toBe('');

    await expect(page.locator('.search-results .hotel-card h3', { hasText: searchResults.lodgings[0].name }).first()).toBeVisible();
    await expect(alert).toHaveCount(0);
  });
});
