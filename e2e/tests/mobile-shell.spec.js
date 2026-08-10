// @ts-check
const { test, expect } = require('../fixtures/fixtures');

/** @param {import('@playwright/test').Locator} locator */
async function expectTouchTarget(locator) {
  const box = await locator.boundingBox();
  expect(box).not.toBeNull();
  expect(box.width).toBeGreaterThanOrEqual(44);
  expect(box.height).toBeGreaterThanOrEqual(44);
}

async function expectNoHorizontalOverflow(page) {
  const viewport = await page.evaluate(() => ({
    clientWidth: document.documentElement.clientWidth,
    scrollWidth: document.documentElement.scrollWidth,
  }));

  expect(viewport.scrollWidth).toBeLessThanOrEqual(viewport.clientWidth);
}

test.describe('Mobile shell — Home', () => {
  test('has no horizontal overflow at 390px', async ({ page, homePage }) => {
    await homePage.open('/');
    await expectNoHorizontalOverflow(page);
  });

  test('opens an accessible menu without covering Home content', async ({ page, homePage }) => {
    await homePage.open('/');

    const openMenuButton = page.getByRole('button', { name: 'Abrir menú' });
    await expectTouchTarget(openMenuButton);
    await expect(openMenuButton).toHaveAttribute('aria-expanded', 'false');
    await expect(openMenuButton).toHaveAttribute('aria-controls', 'mobile-navigation');
    await openMenuButton.click();

    const closeMenuButton = page.getByRole('button', { name: 'Cerrar menú' });
    await expect(closeMenuButton).toBeVisible();
    await expect(closeMenuButton).toHaveAttribute('aria-expanded', 'true');
    await expect(closeMenuButton).toHaveAttribute('aria-controls', 'mobile-navigation');
    await expect(page.locator('#mobile-navigation')).toHaveClass(/nav-links--open/);
    await expect(page.getByRole('link', { name: 'Iniciar sesión' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Crear cuenta' })).toBeVisible();
    await expectNoHorizontalOverflow(page);

    const searchButton = page.locator('main .btn-search');
    await searchButton.scrollIntoViewIfNeeded();
    await expect(searchButton).toBeVisible();
    await searchButton.click({ trial: true });
  });

  test('keeps Header controls at accessible touch sizes', async ({ page, homePage }) => {
    await homePage.open('/');

    const openMenuButton = page.getByRole('button', { name: 'Abrir menú' });
    await expectTouchTarget(openMenuButton);
    await openMenuButton.click();

    for (const link of await page.locator('.nav-links--open a').all()) {
      await expectTouchTarget(link);
    }
  });
});
