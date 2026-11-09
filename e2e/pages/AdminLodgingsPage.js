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
 * Note: the form requires at least one image. createLodging stubs POST /upload
 * and attaches a tiny file, so no real upload happens. categoryId, featureIds
 * and policyIds are optional.
 */
class AdminLodgingsPage extends AdminBasePage {
  /** @param {import('@playwright/test').Page} page */
  constructor(page) {
    super(page);
    this.entityKey = 'lodgings';
    /** @type {'dialog' | 'component'} */
    this.deleteMode = 'component';
  }

  /**
   * Navigate to the Lodgings section in the admin sidebar.
   */
  async goto() {
    await this.gotoEntity(this.entityKey);
  }

  /**
   * Open the add form, fill all required text fields, attach a stubbed image, then save.
   * The upload endpoint is intercepted, so the stored image URL is a fixed placeholder.
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
    await this.page.route(
      (url) => url.pathname.endsWith('/upload'),
      async (route) => {
        if (route.request().method() !== 'POST') return route.continue();
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ url: 'https://example.com/e2e-lodging.png' }),
        });
      },
      { times: 1 },
    );
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
    await this.page.locator('#imageUpload').setInputFiles({
      name: 'e2e-lodging.png',
      mimeType: 'image/png',
      buffer: Buffer.from(
        'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==',
        'base64',
      ),
    });
    await this.page.locator('.image-preview-item').first().waitFor();
    const responsePromise = this.page.waitForResponse(
      (response) => response.url().endsWith('/api/lodgings') && response.request().method() === 'POST',
    );
    await this.save();
    const response = await responsePromise;
    await this.page.locator('[data-testid="admin-modal"]').waitFor({ state: 'hidden' });
    return response;
  }

  /**
   * Delete tracked lodgings through the admin UI. Cleanup failures fail the test.
   * @param {Array<number|string>} ids
   */
  async cleanupCreatedLodgings(ids) {
    return this.cleanupCreatedRows(ids, this.deleteMode);
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
