// @ts-check
const AdminBasePage = require('./AdminBasePage');

/**
 * AdminFeaturesPage — page object for the Admin › Features section.
 *
 * Entity key : "features"
 * Form fields : name (text input), icon (IconPicker — NOT fillField)
 * Delete mode : "dialog" (native window.confirm)
 */
class AdminFeaturesPage extends AdminBasePage {
  /** @param {import('@playwright/test').Page} page */
  constructor(page) {
    super(page);
    this.entityKey = 'features';
    this.deleteMode = 'dialog';
  }

  /**
   * Navigate to the Features section in the admin sidebar.
   */
  async goto() {
    await this.gotoEntity(this.entityKey);
  }

  /**
   * Open the add form, fill name and pick an icon, then save.
   * @param {string} name
   * @param {string} iconKey  — e.g. "fa-solid fa-wifi"
   */
  async createFeature(name, iconKey) {
    await this.openAddForm();
    await this.fillField('name', name);
    await this.pickIcon(iconKey);
    await this.save();
  }

  /**
   * Find the row by id, open edit modal, change the name field, save.
   * @param {number|string} id
   * @param {string} newName
   */
  async editFeatureName(id, newName) {
    await this.editRow(id);
    await this.fillField('name', newName);
    await this.save();
  }

  /**
   * Delete the row identified by id using native window.confirm.
   * @param {number|string} id
   */
  async deleteFeature(id) {
    await this.deleteRow(id, this.deleteMode);
  }
}

module.exports = AdminFeaturesPage;
