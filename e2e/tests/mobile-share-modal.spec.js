// @ts-check
const { test, expect } = require('../fixtures/fixtures');

const LODGING_ID = 8675309;
const lodging = {
  id: LODGING_ID,
  name: 'Cabaña Mobile',
  city: 'Bariloche',
  country: 'Argentina',
  pricePerNight: 120,
  imageUrls: [],
  description: 'Alojamiento determinista para ShareModal.',
  features: [],
};

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
  await locator.scrollIntoViewIfNeeded();
  const box = await locator.boundingBox();
  const viewport = page.viewportSize();
  expect(box).not.toBeNull();
  expect(viewport).not.toBeNull();
  expect(box.x).toBeGreaterThanOrEqual(-1);
  expect(box.y).toBeGreaterThanOrEqual(-1);
  expect(box.x + box.width).toBeLessThanOrEqual(viewport.width + 1);
  expect(box.y + box.height).toBeLessThanOrEqual(viewport.height + 1);
}

/**
 * @param {import('@playwright/test').Page} page
 * @param {import('@playwright/test').Locator} dialog
 */
async function expectNoHorizontalOverflow(page, dialog) {
  const sizes = await page.evaluate(() => ({
    documentClientWidth: document.documentElement.clientWidth,
    documentScrollWidth: document.documentElement.scrollWidth,
  }));
  expect(sizes.documentScrollWidth).toBeLessThanOrEqual(sizes.documentClientWidth);

  if (await dialog.count()) {
    const dialogSize = await dialog.evaluate((element) => ({
      clientWidth: element.clientWidth,
      scrollWidth: element.scrollWidth,
    }));
    expect(dialogSize.scrollWidth).toBeLessThanOrEqual(dialogSize.clientWidth);
  }
}

/** @param {import('@playwright/test').Page} page */
async function exerciseShareModal(page) {
  await interceptProductDetailApi(page);
  await page.goto(`/lodgings/${LODGING_ID}`);

  const opener = page.getByRole('button', { name: 'Compartir' });
  await expect(opener).toBeVisible();
  await opener.evaluate((element) => {
    document.body.style.overflow = 'scroll';
    element.focus();
  });
  await opener.click();

  const dialog = page.getByRole('dialog', { name: 'Compartir' });
  const close = dialog.getByRole('button', { name: 'Cerrar' });
  const copy = dialog.getByRole('button', { name: 'Copiar enlace' });
  const message = dialog.getByLabel('Mensaje para compartir');
  const actions = [
    close,
    copy,
    message,
    dialog.getByRole('link', { name: 'Facebook' }),
    dialog.getByRole('link', { name: 'Twitter' }),
    dialog.getByRole('link', { name: 'WhatsApp' }),
    dialog.getByRole('button', { name: 'Instagram' }),
  ];

  await expect(dialog).toHaveAttribute('aria-modal', 'true');
  await expect(dialog).toHaveAccessibleDescription('Elegí cómo enviar este alojamiento.');
  await expect(close).toBeFocused();
  await expect.poll(() => page.evaluate(() => document.body.style.overflow)).toBe('hidden');
  await expectNoHorizontalOverflow(page, dialog);

  for (const action of actions) {
    await expect(action).toBeVisible();
    await expectTouchTarget(action);
    await expectContainedInViewport(page, action);
  }

  await close.focus();
  for (const action of actions.slice(1)) {
    await page.keyboard.press('Tab');
    await expect(action).toBeFocused();
  }
  await page.keyboard.press('Tab');
  await expect(close).toBeFocused();
  await page.keyboard.press('Shift+Tab');
  await expect(actions.at(-1)).toBeFocused();

  await message.fill('Mensaje mobile determinista');
  await expectNoHorizontalOverflow(page, dialog);
  await expect.poll(() => page.evaluate(() => document.body.style.overflow)).toBe('hidden');

  await page.keyboard.press('Escape');
  await expect(dialog).toHaveCount(0);
  await expect(opener).toBeFocused();
  await expect.poll(() => page.evaluate(() => document.body.style.overflow)).toBe('scroll');
  await expectNoHorizontalOverflow(page, dialog);
}

test.describe('Mobile ShareModal', () => {
  test('keeps every action accessible at the project mobile viewport', async ({ page }) => {
    await exerciseShareModal(page);
  });
});

test.describe('Mobile ShareModal at 320x844', () => {
  test.use({ viewport: { width: 320, height: 844 } });

  test('keeps every action accessible without overflow', async ({ page }) => {
    await exerciseShareModal(page);
  });
});
