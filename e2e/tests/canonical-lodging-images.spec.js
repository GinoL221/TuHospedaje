// @ts-check
const { test, expect } = require('@playwright/test');

/**
 * @typedef {object} CatalogResponse
 * @property {number} totalItems
 * @property {{ id: number, imageUrls: string[] }[]} lodgings
 */

test.describe('Canonical lodging images', () => {
  test.skip(
    process.env.CANONICAL_ASSETS_E2E !== '1',
    'This test requires external canonical lodging assets.',
  );

  test('all 38 seeded lodgings expose five loadable canonical images', async ({ page }) => {
    test.setTimeout(180_000);

    const apiBaseUrl = (process.env.CANONICAL_API_URL || 'http://localhost:8080/api').replace(/\/$/, '');
    const catalogUrl = new URL(`${apiBaseUrl}/lodgings/search`);
    catalogUrl.search = new URLSearchParams({ page: '0', size: '100' }).toString();

    const catalogResponse = await page.request.get(catalogUrl.toString());
    expect(catalogResponse.ok()).toBe(true);

    const catalog = /** @type {CatalogResponse} */ (await catalogResponse.json());
    const lodgingIds = catalog.lodgings.map((lodging) => lodging.id);

    expect(catalog.totalItems).toBe(38);
    expect(lodgingIds).toHaveLength(38);
    expect(new Set(lodgingIds).size).toBe(38);

    for (const lodging of catalog.lodgings) {
      expect(lodging.imageUrls).toHaveLength(5);
      await page.goto(`/lodgings/${lodging.id}`);
      await expect(page.locator('.product-detail h1')).toBeVisible();

      const mainImage = page.locator('.gallery-main-trigger img');
      const thumbnails = page.locator('.gallery-thumbs img');
      await expect(mainImage).toHaveCount(1);
      await expect(thumbnails).toHaveCount(4);

      const images = page.locator('.gallery-main-trigger img, .gallery-thumbs img');
      const imageUrls = await images.evaluateAll((elements) =>
        elements.map((image) => image.getAttribute('src')),
      );
      expect(imageUrls).toEqual(lodging.imageUrls);
      expect(new Set(imageUrls).size).toBe(5);

      for (let index = 0; index < 5; index += 1) {
        await images.nth(index).scrollIntoViewIfNeeded();
      }

      await expect.poll(
        () => images.evaluateAll((elements, expectedUrls) =>
          elements.length === expectedUrls.length && elements.every((element, index) => {
            if (!(element instanceof HTMLImageElement)) return false;

            return element.complete
              && element.naturalWidth > 0
              && element.naturalHeight > 0
              && element.currentSrc === expectedUrls[index]
              && new URL(element.currentSrc).pathname.startsWith('/canonical-lodging-images/');
          }), lodging.imageUrls),
        { message: `canonical images for lodging ${lodging.id} should load in the browser` },
      ).toBe(true);
    }
  });
});
