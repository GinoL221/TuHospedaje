// @ts-check
const { test, expect } = require('@playwright/test');

/**
 * @typedef {object} CatalogResponse
 * @property {number} totalItems
 * @property {{ id: number }[]} lodgings
 */

test.describe('Canonical lodging images', () => {
  test.skip(
    process.env.CANONICAL_ASSETS_E2E !== '1',
    'This test requires external canonical lodging assets.',
  );

  test('all 38 seeded lodgings expose five loadable canonical images', async ({ page }) => {
    test.setTimeout(180_000);

    const initialCatalogResponse = page.waitForResponse((response) => {
      const url = new URL(response.url());
      return response.request().method() === 'GET'
        && url.pathname.endsWith('/api/lodgings/search');
    });

    await page.goto('/search');
    const searchResponse = await initialCatalogResponse;
    expect(searchResponse.ok()).toBe(true);

    const catalogUrl = new URL(searchResponse.url());
    catalogUrl.search = new URLSearchParams({ page: '0', size: '100' }).toString();

    const catalogResponse = await page.request.get(catalogUrl.toString());
    expect(catalogResponse.ok()).toBe(true);

    const catalog = /** @type {CatalogResponse} */ (await catalogResponse.json());
    const lodgingIds = catalog.lodgings.map((lodging) => lodging.id);

    expect(catalog.totalItems).toBe(38);
    expect(lodgingIds).toHaveLength(38);
    expect(new Set(lodgingIds).size).toBe(38);

    for (const lodgingId of lodgingIds) {
      await page.goto(`/lodgings/${lodgingId}`);
      await expect(page.locator('.product-detail h1')).toBeVisible();

      const images = page.locator('.gallery-thumbs img');
      await expect(images).toHaveCount(5);

      const imageUrls = await images.evaluateAll((elements) =>
        elements.map((image) => image.getAttribute('src')),
      );

          expect(new Set(imageUrls).size).toBe(5);
          for (const imageUrl of imageUrls) {
            if (!imageUrl) {
              throw new Error(`lodging ${lodgingId} rendered an image without a source`);
            }
            expect(new URL(imageUrl, page.url()).pathname).toMatch(/^\/canonical-lodging-images\//);
          }

      for (let index = 0; index < 5; index += 1) {
        await images.nth(index).scrollIntoViewIfNeeded();
      }

      await expect.poll(
            () => images.evaluateAll((elements) =>
              elements.every((element) => {
                if (!(element instanceof HTMLImageElement)) {
                  return false;
                }

                return element.complete
                  && element.naturalWidth > 0
                  && element.naturalHeight > 0
                  && new URL(element.currentSrc || element.src).pathname.startsWith('/canonical-lodging-images/');
              }),
            ),
        { message: `canonical images for lodging ${lodgingId} should load in the browser` },
      ).toBe(true);
    }
  });
});
