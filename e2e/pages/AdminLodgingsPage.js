// @ts-check
const AdminBasePage = require('./AdminBasePage');

/**
 * AdminLodgingsPage — page object for the Admin › Lodgings section.
 *
 * Entity key : "lodgings"
 * Form fields : name, email, description, address, city, country, phoneNumber
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
    await this.save();
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
