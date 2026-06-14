// @ts-check
/**
 * Admin › Categories — full CRUD E2E spec (PR-B).
 *
 * Scenarios:
 *   1. Create happy path — fill name only → row appears in table
 *   2. Edit happy path   — open edit, change name → updated name in table
 *   3. Delete happy path — delete (ConfirmDialog) → row gone from table
 *   4. Validation        — submit empty form → error-name visible
 *
 * Skips cleanly when the admin shell is not reachable (stack down).
 * Uses unique timestamped names so concurrent/repeated runs never collide.
 * afterEach safety-net removes any leftover rows whose text starts with the prefix.
 */

const { test, expect } = require('../fixtures/fixtures');
const AdminCategoriesPage = require('../pages/AdminCategoriesPage');

test.describe('Admin › Categories CRUD', () => {
  /** @type {AdminCategoriesPage} */
  let categoriesPage;

  test.beforeEach(async ({ adminUser }) => {
    const { page } = adminUser;

    const shellPresent = await page
      .locator('[data-testid="admin-nav-categories"]')
      .isVisible()
      .catch(() => false);

    if (!shellPresent) {
      test.skip(true, 'Admin shell not accessible — stack may be down');
      return;
    }

    categoriesPage = new AdminCategoriesPage(page);
    await categoriesPage.goto();
  });

  test.afterEach(async ({ adminUser }) => {
    if (!categoriesPage) return;
    await categoriesPage.afterEachCleanup('e2e-cat-', 'component');
  });

  test('creates a category with name only and the row appears in the table', async ({ adminUser }) => {
    const ts = Date.now();
    const name = `e2e-cat-${ts}`;

    await categoriesPage.openAddForm();
    await categoriesPage.fillField('name', name);
    await categoriesPage.save();

    // The modal closes and the new row is visible in the table.
    await expect(
      categoriesPage.findRowByText(name),
      `Expected row with name "${name}" to appear in the categories table`,
    ).toBeVisible();
  });

  test('edits a category name and the table reflects the change', async ({ adminUser }) => {
    const ts = Date.now();
    const originalName = `e2e-cat-${ts}`;
    const updatedName = `e2e-cat-${ts}-edited`;

    // Create the record first.
    await categoriesPage.openAddForm();
    await categoriesPage.fillField('name', originalName);
    await categoriesPage.save();

    // Locate the newly created row and get its id from data-testid.
    const row = categoriesPage.findRowByText(originalName);
    await expect(row).toBeVisible();
    const testid = await row.getAttribute('data-testid');
    const id = testid ? testid.replace('row-', '') : null;
    if (!id) throw new Error('Could not determine row id after create');

    // Open edit, change name, save.
    await categoriesPage.editRow(id);
    const nameField = adminUser.page.locator('[data-testid="field-name"]');
    await nameField.fill(updatedName);
    await categoriesPage.save();

    // Updated name is now visible; original is gone.
    await expect(
      categoriesPage.findRowByText(updatedName),
      `Expected updated name "${updatedName}" to appear in the table`,
    ).toBeVisible();
    await expect(
      categoriesPage.findRowByText(originalName),
    ).not.toBeVisible();
  });

  test('deletes a category via ConfirmDialog and the row is removed from the table', async ({ adminUser }) => {
    const ts = Date.now();
    const name = `e2e-cat-${ts}`;

    // Create.
    await categoriesPage.openAddForm();
    await categoriesPage.fillField('name', name);
    await categoriesPage.save();

    const row = categoriesPage.findRowByText(name);
    await expect(row).toBeVisible();
    const testid = await row.getAttribute('data-testid');
    const id = testid ? testid.replace('row-', '') : null;
    if (!id) throw new Error('Could not determine row id after create');

    // Delete — ConfirmDialog "confirm-delete-yes" is clicked inside deleteRow("component").
    await categoriesPage.deleteRow(id, 'component');

    // Row must be gone.
    await expect(
      categoriesPage.findRowByText(name),
      `Expected row with name "${name}" to be removed after deletion`,
    ).not.toBeVisible();
  });

  test('shows name validation error when saving an empty form', async ({ adminUser }) => {
    await categoriesPage.openAddForm();
    // Click save without filling anything.
    await categoriesPage.save();

    // error-name must be visible.
    await categoriesPage.expectFieldError('name');
  });
});
