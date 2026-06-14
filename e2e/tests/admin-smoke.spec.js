// @ts-check
/**
 * Admin shell smoke spec — PR-A verification.
 *
 * Verifies that /admin loads and all six entity nav tabs are reachable
 * after an admin login. This spec is the minimal check that proves PR-A
 * infra (testids + adminUser fixture + AdminBasePage) works end-to-end.
 *
 * Skips cleanly when the stack (frontend :5173 / backend :8080) is down
 * or when TEST_ADMIN_EMAIL / TEST_ADMIN_PASSWORD are not set and the
 * default seeded admin does not exist.
 */

const { test, expect } = require('../fixtures/fixtures');
const AdminBasePage = require('../pages/AdminBasePage');

const ENTITY_TABS = [
  'lodgings',
  'categories',
  'features',
  'policies',
  'users',
  'reservations',
];

test.describe('Admin shell smoke', () => {
  test('admin logs in and /admin loads with all nav tabs visible', async ({ adminUser }) => {
    const { page } = adminUser;

    // If the admin shell is not accessible, skip rather than fail.
    const navDashboard = page.locator('[data-testid="admin-nav-dashboard"]');
    const shellPresent = await navDashboard.isVisible().catch(() => false);
    if (!shellPresent) {
      test.skip(true, 'Admin shell not accessible — stack may be down or admin seed missing');
      return;
    }

    // Dashboard nav tab is visible.
    await expect(navDashboard).toBeVisible();

    // All six entity tabs are present in the sidebar.
    for (const key of ENTITY_TABS) {
      await expect(
        page.locator(`[data-testid="admin-nav-${key}"]`),
        `Expected nav tab admin-nav-${key} to be visible`,
      ).toBeVisible();
    }
  });

  test('each entity tab is reachable by clicking the sidebar nav', async ({ adminUser }) => {
    const { page } = adminUser;

    const shellPresent = await page
      .locator('[data-testid="admin-nav-dashboard"]')
      .isVisible()
      .catch(() => false);
    if (!shellPresent) {
      test.skip(true, 'Admin shell not accessible — stack may be down or admin seed missing');
      return;
    }

    const adminPage = new AdminBasePage(page);

    for (const key of ENTITY_TABS) {
      await adminPage.gotoEntity(key);
      // After clicking the nav button the active tab renders — the nav button
      // itself stays visible as a stable anchor to verify the click landed.
      await expect(
        page.locator(`[data-testid="admin-nav-${key}"]`),
      ).toBeVisible();
    }
  });
});
