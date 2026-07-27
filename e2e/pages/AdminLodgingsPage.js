// @ts-check
const AdminBasePage = require('./AdminBasePage');

/**
 * AdminLodgingsPage — page object for the Admin › Lodgings section.
 *
 * Entity key : "lodgings"
 * Form fields : name, email, description, address, city, country, phoneNumber,
 *               pricePerNight, maxGuests
 *               (all text inputs / textarea via LodgingFormModal)
 * Delete mode : "component" (ConfirmDialog — confirm-delete-yes)
 *
 * Note: image upload is OUT OF SCOPE — Cloudinary is not available in test environments.
 * All fields listed above are required; image, categoryId, featureIds, policyIds are optional.
 */
class AdminLodgingsPage extends AdminBasePage {
  /** @param {import('@playwright/test').Page} page */
  constructor(page) {
    super(page);
    this.entityKey = 'lodgings';
    this.deleteMode = 'component';
  }

  /**
   * Navigate to the Lodgings section in the admin sidebar.
   */
  async goto() {
    await this.gotoEntity(this.entityKey);
  }

  /**
   * Open the add form, fill all required text fields, then save.
   * Image upload is intentionally skipped (out of scope for E2E).
   * @param {object} fields
   * @param {string} fields.name
   * @param {string} fields.email
   * @param {string} fields.description
   * @param {string} fields.address
   * @param {string} fields.city
   * @param {string} fields.country
   * @param {string} fields.phoneNumber
   * @param {number} fields.pricePerNight
   * @param {number} fields.maxGuests
   */
  async createLodging(fields) {
    await this.openAddForm();
    await this.fillField('name', fields.name);
    await this.fillField('email', fields.email);
    await this.fillField('description', fields.description);
    await this.fillField('address', fields.address);
    await this.fillField('city', fields.city);
    await this.fillField('country', fields.country);
    await this.fillField('phoneNumber', fields.phoneNumber);
    await this.fillField('pricePerNight', String(fields.pricePerNight));
    await this.fillField('maxGuests', String(fields.maxGuests));
    const responsePromise = this.page.waitForResponse(
      (response) => response.url().endsWith('/api/lodgings') && response.request().method() === 'POST',
    );
    await this.save();
    const response = await responsePromise;
    await this.page.locator('[data-testid="admin-modal"]').waitFor({ state: 'hidden' });
    return response;
  }

  /**
   * Find a newly created lodging by id, navigating to the last page when needed.
   * The admin table is sorted by id ascending by default.
   * @param {number|string} id
   */
  async findCreatedRow(id) {
    const row = this.findRow(id);
    if (await row.isVisible()) return row;

    const lastPageButton = this.page.getByRole('button', { name: 'Última', exact: true });
    if (await lastPageButton.isVisible() && await lastPageButton.isEnabled()) {
      await lastPageButton.click();
    }

    await row.waitFor({ state: 'visible' });
    return row;
  }

  /**
   * Delete tracked lodgings through the admin UI. Cleanup failures fail the test.
   * @param {Array<number|string>} ids
   */
  async cleanupCreatedLodgings(ids) {
    for (const id of ids) {
      const row = await this.findCreatedRow(id);
      await this.deleteLodging(id);
      await row.waitFor({ state: 'detached' });
    }
  }

  /**
   * Find the row by id, open edit modal, change the name field, save.
   * @param {number|string} id
   * @param {string} newName
   */
  async editLodgingName(id, newName) {
    await this.editRow(id);
    await this.fillField('name', newName);
    await this.save();
  }

  /**
   * Delete the row identified by id using the ConfirmDialog component.
   * @param {number|string} id
   */
  async deleteLodging(id) {
    await this.deleteRow(id, this.deleteMode);
  }
}

module.exports = AdminLodgingsPage;
