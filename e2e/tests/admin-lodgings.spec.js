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
 * Fails with the current URL when the admin shell is not reachable.
 * Image upload is OUT OF SCOPE — Cloudinary is not available in test environments.
 * Uses unique timestamped names and emails so concurrent/repeated runs never collide.
 * afterEach removes tracked created ids through the admin UI.
 */

const { test, expect } = require('../fixtures/fixtures');
const AdminLodgingsPage = require('../pages/AdminLodgingsPage');

/** Minimal valid lodging fields — no image, no category, no features, no policies. */
const BASE_LODGING = {
  description: 'E2E test lodging description',
  address: '123 Test Street',
  city: 'Test City',
  country: 'Test Country',
  phoneNumber: '+1234567890',
  pricePerNight: 45000.5,
  maxGuests: 5,
};

test.describe('Admin › Lodgings CRUD', () => {
  /** @type {AdminLodgingsPage} */
  let lodgingsPage;
  /** @type {Set<number|string>} */
  let createdIds;

  /** @param {string} suffix */
  async function createTrackedLodging(suffix) {
    const token = `${Date.now()}-${suffix}`;
    const fields = {
      ...BASE_LODGING,
      name: `e2e-lodg-${token}`,
      email: `e2e-lodging-${token}@test.com`,
    };
    const response = await lodgingsPage.createLodging(fields);
    expect(response.ok(), `Create lodging failed with HTTP ${response.status()}`).toBe(true);
    const created = await response.json();
    expect(created.id, 'Create lodging response must include an id').toBeDefined();
    createdIds.add(created.id);
    return { created, fields };
  }

  test.beforeEach(async ({ adminUser }) => {
    const { page } = adminUser;
    const currentUrl = new URL(page.url());
    await expect(
      page.locator('[data-testid="admin-nav-lodgings"]'),
      `[admin-lodgings] stage=admin-nav-lodgings currentUrl=${currentUrl.origin}${currentUrl.pathname}`,
    ).toBeVisible();

    lodgingsPage = new AdminLodgingsPage(page);
    createdIds = new Set();
    await lodgingsPage.goto();
  });

  test.afterEach(async ({ adminUser }) => {
    if (!lodgingsPage || !createdIds) return;
    await lodgingsPage.cleanupCreatedLodgings([...createdIds]);
  });

  test('creates a lodging (image-less) and the row appears in the table', async ({ adminUser }) => {
    const { created, fields } = await createTrackedLodging('create');

    expect(created.pricePerNight).toBe(BASE_LODGING.pricePerNight);
    expect(created.maxGuests).toBe(BASE_LODGING.maxGuests);

    const row = await lodgingsPage.findCreatedRow(created.id);
    await expect(
      row,
      `Expected row-${created.id} to be visible in the lodgings table`,
    ).toBeVisible();
    await expect(row).toContainText(fields.name);
  });

  test('edits a lodging name and the table reflects the change', async ({ adminUser }) => {
    const { created, fields } = await createTrackedLodging('edit');
    // Derived from fields.name (not a separately-timed string) so it can never
    // contain the original as a substring, regardless of timestamp collisions.
    const updatedName = fields.name.replace('e2e-lodg-', 'e2e-lodg-updated-');
    const row = await lodgingsPage.findCreatedRow(created.id);

    await lodgingsPage.editRow(created.id);
    await lodgingsPage.fillField('name', updatedName);
    await lodgingsPage.save();

    await expect(row, `Expected row-${created.id} to show its updated name`).toContainText(updatedName);
    await expect(row).not.toContainText(fields.name);
  });

  test('deletes a lodging via ConfirmDialog and the row is removed from the table', async ({ adminUser }) => {
    const { created } = await createTrackedLodging('delete');
    const row = await lodgingsPage.findCreatedRow(created.id);

    await lodgingsPage.deleteLodging(created.id);

    await expect(
      row,
      `Expected row-${created.id} to be removed after deletion`,
    ).not.toBeVisible();
    createdIds.delete(created.id);
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
    await lodgingsPage.expectFieldError('pricePerNight');
    await lodgingsPage.expectFieldError('maxGuests');
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
    await lodgingsPage.fillField('pricePerNight', String(BASE_LODGING.pricePerNight));
    await lodgingsPage.fillField('maxGuests', String(BASE_LODGING.maxGuests));
    await lodgingsPage.save();

    // Only email error should appear.
    await lodgingsPage.expectFieldError('email');
  });
});
