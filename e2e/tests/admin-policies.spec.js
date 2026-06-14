// @ts-check
/**
 * Admin › Policies — full CRUD E2E spec (PR-C).
 *
 * Scenarios:
 *   1. Create happy path — fill name + pick icon → row appears in table
 *   2. Edit happy path   — open edit, change name → updated name in table
 *   3. Delete happy path — delete (window.confirm) → row gone from table
 *   4. Validation        — submit empty form → error-name visible
 *
 * Skips cleanly when the admin shell is not reachable (stack down).
 * Uses unique timestamped names so concurrent/repeated runs never collide.
 * afterEach safety-net removes any leftover rows whose text starts with the prefix.
 */

const { test, expect } = require('../fixtures/fixtures');
const AdminPoliciesPage = require('../pages/AdminPoliciesPage');

// A simple icon that is reliably present in the IconPicker.
const TEST_ICON = 'fa-solid fa-shield';

test.describe('Admin › Policies CRUD', () => {
  /** @type {AdminPoliciesPage} */
  let policiesPage;

  test.beforeEach(async ({ adminUser }) => {
    const { page } = adminUser;

    const shellPresent = await page
      .locator('[data-testid="admin-nav-policies"]')
      .isVisible()
      .catch(() => false);

    if (!shellPresent) {
      test.skip(true, 'Admin shell not accessible — stack may be down');
      return;
    }

    policiesPage = new AdminPoliciesPage(page);
    await policiesPage.goto();
  });

  test.afterEach(async ({ adminUser }) => {
    if (!policiesPage) return;
    await policiesPage.afterEachCleanup('e2e-pol-', 'dialog');
  });

  test('creates a policy and the row appears in the table', async ({ adminUser }) => {
    const ts = Date.now();
    const name = `e2e-pol-${ts}`;

    await policiesPage.openAddForm();
    await policiesPage.fillField('name', name);
    await policiesPage.pickIcon(TEST_ICON);
    await policiesPage.save();

    // The modal closes and the new row is visible in the table.
    await expect(
      policiesPage.findRowByText(name),
      `Expected row with name "${name}" to appear in the policies table`,
    ).toBeVisible();
  });

  test('edits a policy name and the table reflects the change', async ({ adminUser }) => {
    const ts = Date.now();
    const originalName = `e2e-pol-${ts}`;
    const updatedName = `e2e-pol-${ts}-edited`;

    // Create the record first.
    await policiesPage.openAddForm();
    await policiesPage.fillField('name', originalName);
    await policiesPage.pickIcon(TEST_ICON);
    await policiesPage.save();

    // Locate the newly created row and get its id from data-testid.
    const row = policiesPage.findRowByText(originalName);
    await expect(row).toBeVisible();
    const testid = await row.getAttribute('data-testid');
    const id = testid ? testid.replace('row-', '') : null;
    if (!id) throw new Error('Could not determine row id after create');

    // Open edit, change name, save.
    await policiesPage.editRow(id);
    await policiesPage.fillField('name', updatedName);
    await policiesPage.save();

    // Updated name is now visible; original is gone.
    await expect(
      policiesPage.findRowByText(updatedName),
      `Expected updated name "${updatedName}" to appear in the table`,
    ).toBeVisible();
    await expect(
      policiesPage.findRowByText(originalName),
    ).not.toBeVisible();
  });

  test('deletes a policy via window.confirm and the row is removed from the table', async ({ adminUser }) => {
    const ts = Date.now();
    const name = `e2e-pol-${ts}`;

    // Create.
    await policiesPage.openAddForm();
    await policiesPage.fillField('name', name);
    await policiesPage.pickIcon(TEST_ICON);
    await policiesPage.save();

    const row = policiesPage.findRowByText(name);
    await expect(row).toBeVisible();
    const testid = await row.getAttribute('data-testid');
    const id = testid ? testid.replace('row-', '') : null;
    if (!id) throw new Error('Could not determine row id after create');

    // Delete — window.confirm is accepted inside deleteRow("dialog").
    await policiesPage.deleteRow(id, 'dialog');

    // Row must be gone.
    await expect(
      policiesPage.findRowByText(name),
      `Expected row with name "${name}" to be removed after deletion`,
    ).not.toBeVisible();
  });

  test('shows name validation error when saving an empty form', async ({ adminUser }) => {
    await policiesPage.openAddForm();
    // Click save without filling anything.
    await policiesPage.save();

    // error-name must be visible.
    await policiesPage.expectFieldError('name');
  });
});
