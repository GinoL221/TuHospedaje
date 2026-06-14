// @ts-check
const AdminBasePage = require('./AdminBasePage');

/**
 * AdminCategoriesPage — page object for the Admin › Categories section.
 *
 * Entity key : "categories"
 * Form fields : name (text input), description (textarea), icon (IconPicker — NOT fillField)
 * Delete mode : "component" (ConfirmDialog — confirm-delete-yes)
 *
 * Note: name is required; description and icon are optional.
 * The delete ConfirmDialog uses testId="confirm-delete".
 * The unsaved-changes ConfirmDialog uses testId="confirm-cancel".
 */
class AdminCategoriesPage extends AdminBasePage {
  /** @param {import('@playwright/test').Page} page */
  constructor(page) {
    super(page);
    this.entityKey = 'categories';
    this.deleteMode = 'component';
  }

  /**
   * Navigate to the Categories section in the admin sidebar.
   */
  async goto() {
    await this.gotoEntity(this.entityKey);
  }

  /**
   * Open the add form, fill name (required), optionally fill description and
   * pick an icon, then save.
   * @param {string} name
   * @param {{ description?: string; iconKey?: string }} [opts]
   */
  async createCategory(name, opts = {}) {
    await this.openAddForm();
    await this.fillField('name', name);
    if (opts.description) {
      await this.fillField('description', opts.description);
    }
    if (opts.iconKey) {
      await this.pickIcon(opts.iconKey);
    }
    await this.save();
  }

  /**
   * Find the row by id, open edit modal, change the name field, save.
   * @param {number|string} id
   * @param {string} newName
   */
  async editCategoryName(id, newName) {
    await this.editRow(id);
    await this.fillField('name', newName);
    await this.save();
  }

  /**
   * Delete the row identified by id using the ConfirmDialog component.
   * @param {number|string} id
   */
  async deleteCategory(id) {
    await this.deleteRow(id, this.deleteMode);
  }
}

module.exports = AdminCategoriesPage;
