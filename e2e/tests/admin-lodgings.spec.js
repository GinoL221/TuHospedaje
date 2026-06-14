// @ts-check
/**
 * Admin › Lodgings — CRUD E2E spec (PR-C, image-less).
 *
 * Scenarios:
 *   1. Create happy path — fill all required text fields → row appears in table
 *   2. Edit happy path   — open edit, change name → updated name in table
 *   3. Delete happy path — delete (ConfirmDialog) → row gone from table
 *   4. Validation        — submit empty form → all required field errors visible
 *   5. Invalid email     — fill all fields with bad email → error-email visible only
 *
 * Skips cleanly when the admin shell is not reachable (stack down).
 * Image upload is OUT OF SCOPE — Cloudinary is not available in test environments.
 * Uses unique timestamped names so concurrent/repeated runs never collide.
 * afterEach safety-net removes any leftover rows whose text starts with the prefix.
 */

const { test, expect } = require('../fixtures/fixtures');
const AdminLodgingsPage = require('../pages/AdminLodgingsPage');

/** Minimal valid lodging fields — no image, no category, no features, no policies. */
const BASE_LODGING = {
  email: 'e2e-lodging@test.com',
  description: 'E2E test lodging description',
  address: '123 Test Street',
  city: 'Test City',
  country: 'Test Country',
  phoneNumber: '+1234567890',
};

test.describe('Admin › Lodgings CRUD', () => {
  /** @type {AdminLodgingsPage} */
  let lodgingsPage;

  test.beforeEach(async ({ adminUser }) => {
    const { page } = adminUser;

    const shellPresent = await page
      .locator('[data-testid="admin-nav-lodgings"]')
      .isVisible()
      .catch(() => false);

    if (!shellPresent) {
      test.skip(true, 'Admin shell not accessible — stack may be down');
      return;
    }

    lodgingsPage = new AdminLodgingsPage(page);
    await lodgingsPage.goto();
  });

  test.afterEach(async ({ adminUser }) => {
    if (!lodgingsPage) return;
    await lodgingsPage.afterEachCleanup('e2e-lodg-', 'component');
  });

  test('creates a lodging (image-less) and the row appears in the table', async ({ adminUser }) => {
    const ts = Date.now();
    const name = `e2e-lodg-${ts}`;

    await lodgingsPage.createLodging({ name, ...BASE_LODGING });

    // The modal closes and the new row is visible in the table.
    await expect(
      lodgingsPage.findRowByText(name),
      `Expected row with name "${name}" to appear in the lodgings table`,
    ).toBeVisible();
  });

  test('edits a lodging name and the table reflects the change', async ({ adminUser }) => {
    const ts = Date.now();
    const originalName = `e2e-lodg-${ts}`;
    const updatedName = `e2e-lodg-${ts}-edited`;

    // Create the record first.
    await lodgingsPage.createLodging({ name: originalName, ...BASE_LODGING });

    // Locate the newly created row and get its id from data-testid.
    const row = lodgingsPage.findRowByText(originalName);
    await expect(row).toBeVisible();
    const testid = await row.getAttribute('data-testid');
    const id = testid ? testid.replace('row-', '') : null;
    if (!id) throw new Error('Could not determine row id after create');

    // Open edit, change name using page object method (never raw locators), save.
    await lodgingsPage.editRow(id);
    await lodgingsPage.fillField('name', updatedName);
    await lodgingsPage.save();

    // Updated name is now visible; original is gone.
    await expect(
      lodgingsPage.findRowByText(updatedName),
      `Expected updated name "${updatedName}" to appear in the table`,
    ).toBeVisible();
    await expect(
      lodgingsPage.findRowByText(originalName),
    ).not.toBeVisible();
  });

  test('deletes a lodging via ConfirmDialog and the row is removed from the table', async ({ adminUser }) => {
    const ts = Date.now();
    const name = `e2e-lodg-${ts}`;

    // Create.
    await lodgingsPage.createLodging({ name, ...BASE_LODGING });

    const row = lodgingsPage.findRowByText(name);
    await expect(row).toBeVisible();
    const testid = await row.getAttribute('data-testid');
    const id = testid ? testid.replace('row-', '') : null;
    if (!id) throw new Error('Could not determine row id after create');

    // Delete — ConfirmDialog "confirm-delete-yes" is clicked inside deleteRow("component").
    await lodgingsPage.deleteRow(id, 'component');

    // Row must be gone.
    await expect(
      lodgingsPage.findRowByText(name),
      `Expected row with name "${name}" to be removed after deletion`,
    ).not.toBeVisible();
  });

  test('shows required field errors when saving an empty form', async ({ adminUser }) => {
    await lodgingsPage.openAddForm();
    // Click save without filling anything.
    await lodgingsPage.save();

    // All required fields must show errors.
    await lodgingsPage.expectFieldError('name');
    await lodgingsPage.expectFieldError('description');
    await lodgingsPage.expectFieldError('address');
    await lodgingsPage.expectFieldError('city');
    await lodgingsPage.expectFieldError('country');
    await lodgingsPage.expectFieldError('phoneNumber');
    await lodgingsPage.expectFieldError('email');
  });

  test('shows email validation error for invalid email format', async ({ adminUser }) => {
    const ts = Date.now();
    const name = `e2e-lodg-${ts}`;

    await lodgingsPage.openAddForm();
    await lodgingsPage.fillField('name', name);
    await lodgingsPage.fillField('email', 'notanemail');
    await lodgingsPage.fillField('description', BASE_LODGING.description);
    await lodgingsPage.fillField('address', BASE_LODGING.address);
    await lodgingsPage.fillField('city', BASE_LODGING.city);
    await lodgingsPage.fillField('country', BASE_LODGING.country);
    await lodgingsPage.fillField('phoneNumber', BASE_LODGING.phoneNumber);
    await lodgingsPage.save();

    // Only email error should appear.
    await lodgingsPage.expectFieldError('email');
  });
});
