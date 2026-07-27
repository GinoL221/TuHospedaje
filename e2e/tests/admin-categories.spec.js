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
 * Fails with the current URL when the admin shell is not reachable.
 * Uses unique timestamped names so concurrent/repeated runs never collide.
 * Tracks created rows by id (from the create API response) so cleanup and
 * row lookup work regardless of which table page a row ends up on.
 */

const { test, expect } = require('../fixtures/fixtures');
const AdminCategoriesPage = require('../pages/AdminCategoriesPage');

test.describe('Admin › Categories CRUD', () => {
  /** @type {AdminCategoriesPage} */
  let categoriesPage;
  /** @type {Set<number|string>} */
  let createdIds;

  /** @param {string} suffix */
  async function createTrackedCategory(suffix) {
    const name = `e2e-cat-${Date.now()}-${suffix}`;
    const response = await categoriesPage.createCategory(name);
    expect(response.ok(), `Create category failed with HTTP ${response.status()}`).toBe(true);
    const created = await response.json();
    expect(created.id, 'Create category response must include an id').toBeDefined();
    createdIds.add(created.id);
    return { created, name };
  }

  test.beforeEach(async ({ adminUser }) => {
    const { page } = adminUser;
    const currentUrl = new URL(page.url());
    await expect(
      page.locator('[data-testid="admin-nav-categories"]'),
      `[admin-categories] stage=admin-nav-categories currentUrl=${currentUrl.origin}${currentUrl.pathname}`,
    ).toBeVisible();

    categoriesPage = new AdminCategoriesPage(page);
    createdIds = new Set();
    await categoriesPage.goto();
  });

  test.afterEach(async ({ adminUser }) => {
    if (!categoriesPage || !createdIds) return;
    await categoriesPage.cleanupCreatedRows([...createdIds], categoriesPage.deleteMode);
  });

  test('creates a category with name only and the row appears in the table', async ({ adminUser }) => {
    const { created, name } = await createTrackedCategory('create');

    const row = await categoriesPage.findCreatedRow(created.id);
    await expect(
      row,
      `Expected row-${created.id} to appear in the categories table`,
    ).toBeVisible();
    await expect(row).toContainText(name);
  });

  test('edits a category name and the table reflects the change', async ({ adminUser }) => {
    const { created, name } = await createTrackedCategory('edit');
    // Derived from name (not a suffixed variant of it) so it can never
    // contain the original as a substring — a suffix like `${name}-edited`
    // would make the not.toContainText() assertion below false-fail, since
    // the row's text would still contain the original name.
    const updatedName = name.replace('e2e-cat-', 'e2e-cat-edited-');
    const row = await categoriesPage.findCreatedRow(created.id);

    await categoriesPage.editRow(created.id);
    await categoriesPage.fillField('name', updatedName);
    await categoriesPage.save();

    await expect(row, `Expected row-${created.id} to show its updated name`).toContainText(updatedName);
    await expect(row).not.toContainText(name);
  });

  test('deletes a category via ConfirmDialog and the row is removed from the table', async ({ adminUser }) => {
    const { created } = await createTrackedCategory('delete');
    const row = await categoriesPage.findCreatedRow(created.id);

    await categoriesPage.deleteRow(created.id, categoriesPage.deleteMode);

    await expect(
      row,
      `Expected row-${created.id} to be removed after deletion`,
    ).not.toBeVisible();
    createdIds.delete(created.id);
  });

  test('shows name validation error when saving an empty form', async ({ adminUser }) => {
    await categoriesPage.openAddForm();
    // Click save without filling anything.
    await categoriesPage.save();

    // error-name must be visible.
    await categoriesPage.expectFieldError('name');
  });
});
