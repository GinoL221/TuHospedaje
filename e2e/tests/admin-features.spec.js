// @ts-check
/**
 * Admin › Features — full CRUD E2E spec (PR-B).
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
const AdminFeaturesPage = require('../pages/AdminFeaturesPage');

// IconPicker keys come from ICON_MAP (frontend/src/utils/iconMap.js) — lucide-react
// icon names such as "wifi", not Font Awesome classes. "wifi" is reliably present.
const TEST_ICON = 'wifi';

test.describe('Admin › Features CRUD', () => {
  /** @type {AdminFeaturesPage} */
  let featuresPage;
  /** @type {Set<number|string>} */
  let createdIds;

  /** @param {string} suffix */
  async function createTrackedFeature(suffix) {
    const name = `e2e-feat-${Date.now()}-${suffix}`;
    const response = await featuresPage.createFeature(name, TEST_ICON);
    expect(response.ok(), `Create feature failed with HTTP ${response.status()}`).toBe(true);
    const created = await response.json();
    expect(created.id, 'Create feature response must include an id').toBeDefined();
    createdIds.add(created.id);
    return { created, name };
  }

  test.beforeEach(async ({ adminUser }) => {
    const { page } = adminUser;
    const currentUrl = new URL(page.url());
    await expect(
      page.locator('[data-testid="admin-nav-features"]'),
      `[admin-features] stage=admin-nav-features currentUrl=${currentUrl.origin}${currentUrl.pathname}`,
    ).toBeVisible();

    featuresPage = new AdminFeaturesPage(page);
    createdIds = new Set();
    await featuresPage.goto();
  });

  test.afterEach(async ({ adminUser }) => {
    if (!featuresPage || !createdIds) return;
    await featuresPage.cleanupCreatedRows([...createdIds], featuresPage.deleteMode);
  });

  test('creates a feature and the row appears in the table', async ({ adminUser }) => {
    const { created, name } = await createTrackedFeature('create');

    const row = await featuresPage.findCreatedRow(created.id);
    await expect(
      row,
      `Expected row-${created.id} to appear in the features table`,
    ).toBeVisible();
    await expect(row).toContainText(name);
  });

  test('edits a feature name and the table reflects the change', async ({ adminUser }) => {
    const { created, name } = await createTrackedFeature('edit');
    // Derived from name so it can never contain the original as a substring
    // (a suffixed variant would make not.toContainText() false-fail).
    const updatedName = name.replace('e2e-feat-', 'e2e-feat-edited-');
    const row = await featuresPage.findCreatedRow(created.id);

    await featuresPage.editRow(created.id);
    await featuresPage.fillField('name', updatedName);
    await featuresPage.save();

    await expect(row, `Expected row-${created.id} to show its updated name`).toContainText(updatedName);
    await expect(row).not.toContainText(name);
  });

  test('deletes a feature via ConfirmDialog and the row is removed from the table', async ({ adminUser }) => {
    const { created } = await createTrackedFeature('delete');
    const row = await featuresPage.findCreatedRow(created.id);

    await featuresPage.deleteRow(created.id, featuresPage.deleteMode);

    await expect(
      row,
      `Expected row-${created.id} to be removed after deletion`,
    ).not.toBeVisible();
    createdIds.delete(created.id);
  });

  test('shows name validation error when saving an empty form', async ({ adminUser }) => {
    await featuresPage.openAddForm();
    // Click save without filling anything.
    await featuresPage.save();

    // error-name must be visible.
    await featuresPage.expectFieldError('name');
  });
});
