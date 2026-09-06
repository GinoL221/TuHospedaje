// @ts-check
const { test, expect } = require('../fixtures/fixtures');

const LODGING_ID = 6;

function imageData(color) {
  return `data:image/svg+xml,${encodeURIComponent(
    `<svg xmlns="http://www.w3.org/2000/svg" width="800" height="600" viewBox="0 0 800 600"><rect width="800" height="600" fill="${color}"/></svg>`,
  )}`;
}

const lodging = {
  id: LODGING_ID,
  name: 'Hotel Internacional',
  city: 'Buenos Aires',
  country: 'Argentina',
  pricePerNight: 120,
  imageUrls: [imageData('#2a9d8f'), imageData('#264653'), imageData('#e9c46a')],
  description: 'Alojamiento de prueba para la cobertura mobile.',
  features: [],
};

/** @param {import('@playwright/test').Locator} locator */
async function expectTouchTarget(locator) {
  const box = await locator.boundingBox();
  expect(box).not.toBeNull();
  expect(box.width).toBeGreaterThanOrEqual(44);
  expect(box.height).toBeGreaterThanOrEqual(44);
}

/**
 * @param {import('@playwright/test').Page} page
 * @param {import('@playwright/test').Locator} locator
 */
async function expectContainedInViewport(page, locator) {
  const box = await locator.boundingBox();
  expect(box).not.toBeNull();

  const viewport = page.viewportSize();
  expect(viewport).not.toBeNull();
  expect(box.x).toBeGreaterThanOrEqual(-1);
  expect(box.y).toBeGreaterThanOrEqual(-1);
  expect(box.x + box.width).toBeLessThanOrEqual(viewport.width + 1);
  expect(box.y + box.height).toBeLessThanOrEqual(viewport.height + 1);
}

/** @param {import('@playwright/test').Page} page */
async function expectNoHorizontalOverflow(page) {
  const documentSize = await page.evaluate(() => ({
    clientWidth: document.documentElement.clientWidth,
    scrollWidth: document.documentElement.scrollWidth,
  }));

  expect(documentSize.scrollWidth).toBeLessThanOrEqual(documentSize.clientWidth);
}

/** @param {import('@playwright/test').Page} page */
async function interceptProductDetailApi(page) {
  await page.route(`**/api/lodgings/${LODGING_ID}`, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(lodging),
    });
  });
  await page.route(`**/api/lodgings/${LODGING_ID}/availability**`, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ occupiedRanges: [] }),
    });
  });
}

/** @param {import('@playwright/test').Page} page */
async function exerciseGallery(page) {
  await interceptProductDetailApi(page);
  await page.goto(`/lodgings/${LODGING_ID}`);

  const opener = page.getByRole('button', { name: 'Abrir galería' });
  await expect(opener).toBeVisible();
  await expectNoHorizontalOverflow(page);
  await opener.focus();
  await opener.click();

  const modal = page.locator('.gallery-modal-overlay');
  const image = modal.locator('.gallery-modal-image img');
  const close = modal.getByRole('button', { name: 'Cerrar galería' });
  const previous = modal.getByRole('button', { name: 'Imagen anterior' });
  const next = modal.getByRole('button', { name: 'Imagen siguiente' });

  await expect(modal).toBeVisible();
  await expect(image).toHaveAttribute('alt', '1 de 3');
  await expect(modal.getByText('1 / 3')).toBeVisible();
  for (const control of [close, previous, next]) {
    await expectTouchTarget(control);
    await expectContainedInViewport(page, control);
  }
  await expectContainedInViewport(page, image);
  await expectNoHorizontalOverflow(page);

  await next.click();
  await expect(image).toHaveAttribute('alt', '2 de 3');
  await expect(modal.getByText('2 / 3')).toBeVisible();
  await expectNoHorizontalOverflow(page);

  await previous.click();
  await expect(image).toHaveAttribute('alt', '1 de 3');
  await close.click();
  await expect(modal).toHaveCount(0);
  await expect(opener).toBeFocused();
  await expectNoHorizontalOverflow(page);
}

test.describe('Mobile GalleryModal', () => {
  test('keeps the gallery controls reachable at the project mobile viewport', async ({ page }) => {
    await exerciseGallery(page);
  });
});

test.describe('Mobile GalleryModal at 320x844', () => {
  test.use({ viewport: { width: 320, height: 844 } });

  test('keeps the gallery controls reachable without overflow', async ({ page }) => {
    await exerciseGallery(page);
  });
});
