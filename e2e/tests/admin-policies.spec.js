// @ts-check
/**
 * Admin › Policies — full CRUD E2E spec (PR-C).
 *
 * Scenarios:
 *   1. Create happy path — fill name + pick icon → row appears in table
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
const AdminPoliciesPage = require('../pages/AdminPoliciesPage');

// IconPicker keys come from ICON_MAP (frontend/src/utils/iconMap.js) — lucide-react
// icon names such as "ban", not Font Awesome classes. "ban" is reliably present.
const TEST_ICON = 'ban';

test.describe('Admin › Policies CRUD', () => {
  /** @type {AdminPoliciesPage} */
  let policiesPage;
  /** @type {Set<number|string>} */
  let createdIds;

  /** @param {string} suffix */
  async function createTrackedPolicy(suffix) {
    const name = `e2e-pol-${Date.now()}-${suffix}`;
    const response = await policiesPage.createPolicy(name, TEST_ICON);
    expect(response.ok(), `Create policy failed with HTTP ${response.status()}`).toBe(true);
    const created = await response.json();
    expect(created.id, 'Create policy response must include an id').toBeDefined();
    createdIds.add(created.id);
    return { created, name };
  }

  test.beforeEach(async ({ adminUser }) => {
    const { page } = adminUser;
    const currentUrl = new URL(page.url());
    await expect(
      page.locator('[data-testid="admin-nav-policies"]'),
      `[admin-policies] stage=admin-nav-policies currentUrl=${currentUrl.origin}${currentUrl.pathname}`,
    ).toBeVisible();

    policiesPage = new AdminPoliciesPage(page);
    createdIds = new Set();
    await policiesPage.goto();
  });

  test.afterEach(async ({ adminUser }) => {
    if (!policiesPage || !createdIds) return;
    await policiesPage.cleanupCreatedRows([...createdIds], policiesPage.deleteMode);
  });

  test('creates a policy and the row appears in the table', async ({ adminUser }) => {
    const { created, name } = await createTrackedPolicy('create');

    const row = await policiesPage.findCreatedRow(created.id);
    await expect(
      row,
      `Expected row-${created.id} to appear in the policies table`,
    ).toBeVisible();
    await expect(row).toContainText(name);
  });

  test('edits a policy name and the table reflects the change', async ({ adminUser }) => {
    const { created, name } = await createTrackedPolicy('edit');
    // Derived from name so it can never contain the original as a substring
    // (a suffixed variant would make not.toContainText() false-fail).
    const updatedName = name.replace('e2e-pol-', 'e2e-pol-edited-');
    const row = await policiesPage.findCreatedRow(created.id);

    await policiesPage.editRow(created.id);
    await policiesPage.fillField('name', updatedName);
    await policiesPage.save();

    await expect(row, `Expected row-${created.id} to show its updated name`).toContainText(updatedName);
    await expect(row).not.toContainText(name);
  });

  test('deletes a policy via ConfirmDialog and the row is removed from the table', async ({ adminUser }) => {
    const { created } = await createTrackedPolicy('delete');
    const row = await policiesPage.findCreatedRow(created.id);

    await policiesPage.deleteRow(created.id, policiesPage.deleteMode);

    await expect(
      row,
      `Expected row-${created.id} to be removed after deletion`,
    ).not.toBeVisible();
    createdIds.delete(created.id);
  });

  test('shows name validation error when saving an empty form', async ({ adminUser }) => {
    await policiesPage.openAddForm();
    // Click save without filling anything.
    await policiesPage.save();

    // error-name must be visible.
    await policiesPage.expectFieldError('name');
  });
});
