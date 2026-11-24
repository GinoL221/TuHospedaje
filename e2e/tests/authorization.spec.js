// @ts-check
/**
 * Admin UI authorization spec (S5).
 *
 * Verifies browser UI role rejection when an unauthenticated visitor or an
 * authenticated ROLE_USER navigates to /admin, the canonical /administración
 * route, or an admin subroute (/admin/lodgings).
 */

const { test, expect } = require('../fixtures/fixtures');

/**
 * Assert that the admin shell and its dashboard navigation anchor are absent.
 * @param {import('@playwright/test').Page} page
 */
async function expectAdminShellHidden(page) {
  await expect(page.locator('[data-testid="admin-nav-dashboard"]')).toHaveCount(0);
  await expect(page.locator('.admin-shell')).toHaveCount(0);
}

test.describe('Admin UI authorization — unauthenticated', () => {
  test('redirects unauthenticated user from /admin to /login without showing admin shell', async ({ page, loginPage }) => {
    await page.goto('/admin');

    await expect(page).toHaveURL(/\/login$/);
    await expect(loginPage.submitButton).toBeVisible();
    await expectAdminShellHidden(page);
  });

  test('redirects unauthenticated user from /administración to /login without showing admin shell', async ({ page, loginPage }) => {
    await page.goto('/administración');

    await expect(page).toHaveURL(/\/login$/);
    await expect(loginPage.submitButton).toBeVisible();
    await expectAdminShellHidden(page);
  });

  test('blocks unauthenticated user on admin subroute /admin/lodgings without showing admin shell', async ({ page }) => {
    await page.goto('/admin/lodgings');

    await expect(page.getByText('La página que buscás no existe.')).toBeVisible();
    await expectAdminShellHidden(page);
  });
});

test.describe('Admin UI authorization — authenticated ROLE_USER', () => {
  test.skip(
    !process.env.TEST_USER_EMAIL || !process.env.TEST_USER_PASSWORD,
    'Set TEST_USER_EMAIL and TEST_USER_PASSWORD in .env',
  );

  test('redirects ROLE_USER from /admin to /unauthorized without showing admin shell', async ({ page, authUser: _authUser }) => {
    await page.goto('/admin');

    await expect(page).toHaveURL(/\/unauthorized$/);
    await expect(page.getByText('No tenés permisos para acceder a esta página.')).toBeVisible();
    await expectAdminShellHidden(page);
  });

  test('redirects ROLE_USER from /administración to /unauthorized without showing admin shell', async ({ page, authUser: _authUser }) => {
    await page.goto('/administración');

    await expect(page).toHaveURL(/\/unauthorized$/);
    await expect(page.getByText('No tenés permisos para acceder a esta página.')).toBeVisible();
    await expectAdminShellHidden(page);
  });

  test('blocks ROLE_USER on admin subroute /admin/lodgings without showing admin shell', async ({ page, authUser: _authUser }) => {
    await page.goto('/admin/lodgings');

    await expect(page.getByText('La página que buscás no existe.')).toBeVisible();
    await expectAdminShellHidden(page);
  });
});
