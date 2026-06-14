// @ts-check
/**
 * Admin › Features — full CRUD E2E spec (PR-B).
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
const AdminFeaturesPage = require('../pages/AdminFeaturesPage');

// A simple icon that is reliably present in the IconPicker.
const TEST_ICON = 'fa-solid fa-star';

test.describe('Admin › Features CRUD', () => {
  /** @type {AdminFeaturesPage} */
  let featuresPage;

  test.beforeEach(async ({ adminUser }) => {
    const { page } = adminUser;

    const shellPresent = await page
      .locator('[data-testid="admin-nav-features"]')
      .isVisible()
      .catch(() => false);

    if (!shellPresent) {
      test.skip(true, 'Admin shell not accessible — stack may be down');
      return;
    }

    featuresPage = new AdminFeaturesPage(page);
    await featuresPage.goto();
  });

  test.afterEach(async ({ adminUser }) => {
    if (!featuresPage) return;
    await featuresPage.afterEachCleanup('e2e-feat-', 'dialog');
  });

  test('creates a feature and the row appears in the table', async ({ adminUser }) => {
    const ts = Date.now();
    const name = `e2e-feat-${ts}`;

    await featuresPage.openAddForm();
    await featuresPage.fillField('name', name);
    await featuresPage.pickIcon(TEST_ICON);
    await featuresPage.save();

    // The modal closes and the new row is visible in the table.
    await expect(
      featuresPage.findRowByText(name),
      `Expected row with name "${name}" to appear in the features table`,
    ).toBeVisible();
  });

  test('edits a feature name and the table reflects the change', async ({ adminUser }) => {
    const ts = Date.now();
    const originalName = `e2e-feat-${ts}`;
    const updatedName = `e2e-feat-${ts}-edited`;

    // Create the record first.
    await featuresPage.openAddForm();
    await featuresPage.fillField('name', originalName);
    await featuresPage.pickIcon(TEST_ICON);
    await featuresPage.save();

    // Locate the newly created row and get its id from data-testid.
    const row = featuresPage.findRowByText(originalName);
    await expect(row).toBeVisible();
    const testid = await row.getAttribute('data-testid');
    const id = testid ? testid.replace('row-', '') : null;
    if (!id) throw new Error('Could not determine row id after create');

    // Open edit, change name, save.
    await featuresPage.editRow(id);
    await featuresPage.fillField('name', updatedName);
    await featuresPage.save();

    // Updated name is now visible; original is gone.
    await expect(
      featuresPage.findRowByText(updatedName),
      `Expected updated name "${updatedName}" to appear in the table`,
    ).toBeVisible();
    await expect(
      featuresPage.findRowByText(originalName),
    ).not.toBeVisible();
  });

  test('deletes a feature via window.confirm and the row is removed from the table', async ({ adminUser }) => {
    const ts = Date.now();
    const name = `e2e-feat-${ts}`;

    // Create.
    await featuresPage.openAddForm();
    await featuresPage.fillField('name', name);
    await featuresPage.pickIcon(TEST_ICON);
    await featuresPage.save();

    const row = featuresPage.findRowByText(name);
    await expect(row).toBeVisible();
    const testid = await row.getAttribute('data-testid');
    const id = testid ? testid.replace('row-', '') : null;
    if (!id) throw new Error('Could not determine row id after create');

    // Delete — window.confirm is accepted inside deleteRow("dialog").
    await featuresPage.deleteRow(id, 'dialog');

    // Row must be gone.
    await expect(
      featuresPage.findRowByText(name),
      `Expected row with name "${name}" to be removed after deletion`,
    ).not.toBeVisible();
  });

  test('shows name validation error when saving an empty form', async ({ adminUser }) => {
    await featuresPage.openAddForm();
    // Click save without filling anything.
    await featuresPage.save();

    // error-name must be visible.
    await featuresPage.expectFieldError('name');
  });
});
