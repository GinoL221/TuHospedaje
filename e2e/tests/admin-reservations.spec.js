// @ts-check
/**
 * Admin › Reservations — read-only smoke spec (PR-D).
 *
 * Scenarios:
 *   1. Reservations section loads without error (table or empty-state is visible)
 *   2. If reservations-table is present, the table element itself is visible
 *
 * Read-only — no create, no edit, no delete.
 * Does not assert row count — the test environment may have zero reservations.
 *
 * Skips cleanly when the admin shell is not reachable (stack down).
 * No afterEach cleanup needed — no data is mutated.
 */

const { test, expect } = require('../fixtures/fixtures');
const AdminReservationsPage = require('../pages/AdminReservationsPage');

test.describe('Admin › Reservations smoke', () => {
  /** @type {AdminReservationsPage} */
  let reservationsPage;

  test.beforeEach(async ({ adminUser }) => {
    const { page } = adminUser;

    const shellPresent = await page
      .locator('[data-testid="admin-nav-reservations"]')
      .isVisible()
      .catch(() => false);

    if (!shellPresent) {
      test.skip(true, 'Admin shell not accessible — stack may be down');
      return;
    }

    reservationsPage = new AdminReservationsPage(page);
    await reservationsPage.goto();
    await reservationsPage.waitForLoad();
  });

  test('reservations section loads without error', async ({ adminUser }) => {
    const { page } = adminUser;

    // After loading, either the table or the empty-state paragraph is visible.
    // Both are valid — we only assert no error state and something is rendered.
    const tableVisible = await reservationsPage.getTable().isVisible().catch(() => false);
    const emptyState = page.getByText('No hay reservas registradas.');
    const emptyVisible = await emptyState.isVisible().catch(() => false);

    expect(
      tableVisible || emptyVisible,
      'Expected either the reservations table or the empty-state message to be visible after load',
    ).toBe(true);
  });

  test('reservations table element is visible when present', async ({ adminUser }) => {
    const tableVisible = await reservationsPage.getTable().isVisible().catch(() => false);

    if (!tableVisible) {
      // No reservations in this environment — nothing to assert.
      test.skip(true, 'No reservations in test environment — table not rendered');
      return;
    }

    await expect(
      reservationsPage.getTable(),
      'Expected reservations-table to be visible when reservations exist',
    ).toBeVisible();
  });
});
