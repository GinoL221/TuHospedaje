// @ts-check
const AdminBasePage = require('./AdminBasePage');

/**
 * AdminPoliciesPage — page object for the Admin › Policies section.
 *
 * Entity key : "policies"
 * Form fields : name (text input), icon (IconPicker — NOT fillField)
 *               description (textarea, optional)
 * Delete mode : "dialog" (native window.confirm — CONFIRMED in PR-A source review)
 *
 * Note: name and icon are required; description is optional.
 * The unsaved-changes ConfirmDialog uses testId="confirm-cancel" (NOT confirm-delete).
 */
class AdminPoliciesPage extends AdminBasePage {
  /** @param {import('@playwright/test').Page} page */
  constructor(page) {
    super(page);
    this.entityKey = 'policies';
    this.deleteMode = 'dialog';
  }

  /**
   * Navigate to the Policies section in the admin sidebar.
   */
  async goto() {
    await this.gotoEntity(this.entityKey);
  }

  /**
   * Open the add form, fill name (required), pick an icon (required),
   * optionally fill description, then save.
   * @param {string} name
   * @param {string} iconKey  — e.g. "fa-solid fa-shield"
   * @param {{ description?: string }} [opts]
   */
  async createPolicy(name, iconKey, opts = {}) {
    await this.openAddForm();
    await this.fillField('name', name);
    if (opts.description) {
      await this.fillField('description', opts.description);
    }
    await this.pickIcon(iconKey);
    await this.save();
  }

  /**
   * Find the row by id, open edit modal, change the name field, save.
   * @param {number|string} id
   * @param {string} newName
   */
  async editPolicyName(id, newName) {
    await this.editRow(id);
    await this.fillField('name', newName);
    await this.save();
  }

  /**
   * Delete the row identified by id using native window.confirm.
   * @param {number|string} id
   */
  async deletePolicy(id) {
    await this.deleteRow(id, this.deleteMode);
  }
}

module.exports = AdminPoliciesPage;
