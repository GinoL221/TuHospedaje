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

// WU10/US-34.1-S3: WhatsApp must stay in the lower/right region without
// causing horizontal overflow at both required narrow breakpoints.
test.describe('Mobile shell — WhatsApp placement', () => {
  test.describe('at 320px', () => {
    test.use({ viewport: { width: 320, height: 844 } });

    test('the WhatsApp button stays visible and reachable with no horizontal overflow', async ({ page, homePage }) => {
      await homePage.open('/');
      const button = page.getByRole('button', { name: 'Contactar por WhatsApp' });
      await expect(button).toBeVisible();
      await expectTouchTarget(button);
      await expectNoHorizontalOverflow(page);

      const box = await button.boundingBox();
      const viewportWidth = page.viewportSize()?.width ?? 320;
      // Right/lower region: right edge within the viewport, positioned in
      // the lower half of the visible screen.
      expect(box.x + box.width).toBeLessThanOrEqual(viewportWidth + 1);
      expect(box.y).toBeGreaterThan(300);
    });
  });

  test.describe('at 390px', () => {
    test.use({ viewport: { width: 390, height: 844 } });

    test('the WhatsApp button stays visible with no horizontal overflow', async ({ page, homePage }) => {
      await homePage.open('/');
      const button = page.getByRole('button', { name: 'Contactar por WhatsApp' });
      await expect(button).toBeVisible();
      await expectNoHorizontalOverflow(page);
    });
  });
});

test.describe('Mobile shell — Reviews clearance', () => {
  for (const viewport of [
    { width: 375, height: 812 },
    { width: 320, height: 844 },
  ]) {
    test(`keeps fragment and programmatic review targets below the open Header at ${viewport.width}px`, async ({ page }) => {
      await page.setViewportSize(viewport);
      await page.goto('/lodgings/1#reviews-1');

      const header = page.locator('.site-header');
      const reviews = page.getByRole('heading', { name: 'Reseñas' });
      await expect(reviews).toBeVisible();
      const [closedHeaderBox, fragmentBox] = await Promise.all([
        header.boundingBox(),
        reviews.boundingBox(),
      ]);
      expect(closedHeaderBox).not.toBeNull();
      expect(fragmentBox).not.toBeNull();
      expect(fragmentBox.y).toBeGreaterThanOrEqual(closedHeaderBox.y + closedHeaderBox.height);

      await page.getByRole('button', { name: 'Abrir menú' }).click();
      await reviews.evaluate((element) => element.scrollIntoView({ block: 'start' }));

      const [headerBox, reviewsBox] = await Promise.all([
        header.boundingBox(),
        reviews.boundingBox(),
      ]);
      expect(headerBox).not.toBeNull();
      expect(reviewsBox).not.toBeNull();
      expect(reviewsBox.y).toBeGreaterThanOrEqual(headerBox.y + headerBox.height);
      await expectNoHorizontalOverflow(page);
    });
  }
});

// WU3/US-9.2-S3: the mobile/touch admin-unavailable state must appear
// (instead of a broken/partial admin layout) at narrow, touch-capable
// viewports, and must be accessible for a direct/deep-link visit.
test.describe('Mobile shell — Admin unavailable state', () => {
  test.use({ hasTouch: true });

  test.describe('at 320px', () => {
    test.use({ viewport: { width: 320, height: 844 } });

    test('shows an accessible, focused unavailable message instead of the admin shell', async ({ page, loginPage }) => {
      // Not the `adminUser` fixture: that fixture itself asserts the full
      // desktop admin shell renders, which never happens under touch+narrow
      // viewport — the exact condition this test verifies.
      const email = process.env.TEST_ADMIN_EMAIL || 'admin@tuhospedaje.com';
      const password = process.env.TEST_ADMIN_PASSWORD || 'Admin1';
      await loginPage.open('/login');
      await loginPage.login(email, password);
      await page.waitForURL('/');

      await page.goto('/administración');
      const status = page.locator('.admin-mobile-block[role="status"]');
      await expect(status).toBeVisible();
      await expect(status.locator('h2')).toHaveText('Panel no disponible en móvil');
      await expect(status.locator('h2')).toBeFocused();
      await expect(page.locator('[data-testid="admin-nav-dashboard"]')).toHaveCount(0);
      await expectNoHorizontalOverflow(page);
    });
  });
});
