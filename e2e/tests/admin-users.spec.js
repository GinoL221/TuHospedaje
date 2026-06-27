// @ts-check
/**
 * Admin › Users — role-toggle smoke spec (PR-D).
 *
 * Scenarios:
 *   1. Users table loads with at least one row (the seeded admin account)
 *   2. Admin's own row has the role-toggle button disabled (cannot change own role)
 *
 * Read-only smoke — does NOT perform actual role toggles to avoid
 * mutating persistent state that could affect other tests or the running demo.
 *
 * Skips cleanly when the admin shell is not reachable (stack down).
 * No afterEach cleanup needed — no data is mutated.
 */

const { test, expect } = require('../fixtures/fixtures');
const AdminUsersPage = require('../pages/AdminUsersPage');

test.describe('Admin › Users smoke', () => {
  /** @type {AdminUsersPage} */
  let usersPage;

  test.beforeEach(async ({ adminUser }) => {
    const { page } = adminUser;

    const shellPresent = await page
      .locator('[data-testid="admin-nav-users"]')
      .isVisible()
      .catch(() => false);

    if (!shellPresent) {
      test.skip(true, 'Admin shell not accessible — stack may be down');
      return;
    }

    usersPage = new AdminUsersPage(page);
    await usersPage.goto();
  });

  test('users table loads with at least one row', async ({ adminUser }) => {
    const { page } = adminUser;

    // Wait for at least one row to appear in the table.
    // The seeded admin account is always present.
    const firstRow = page.locator('tbody tr').first();
    await expect(
      firstRow,
      'Expected at least one user row to be visible after navigation',
    ).toBeVisible();
  });

  test("admin's own row has the role-toggle button disabled", async ({ adminUser }) => {
    const { page, email } = adminUser;

    // Find the row whose email cell matches the logged-in admin's email.
    const adminRow = page.locator('tbody tr').filter({ hasText: email });
    await expect(
      adminRow,
      `Expected a row for the logged-in admin (${email}) to be visible`,
    ).toBeVisible();

    // The role-toggle button on that row must be disabled.
    const roleBtn = adminRow.locator('[data-testid="row-role-btn"]');
    await expect(
      roleBtn,
      'Expected the role-toggle button on the admin\'s own row to be disabled',
    ).toBeDisabled();
  });
});
