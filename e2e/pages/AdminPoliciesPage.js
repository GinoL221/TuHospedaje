// @ts-check
const AdminBasePage = require('./AdminBasePage');

/**
 * AdminPoliciesPage — page object for the Admin › Policies section.
 *
 * Entity key : "policies"
 * Form fields : name (text input), icon (IconPicker — NOT fillField)
 *               description (textarea, optional)
 * Delete mode : "component" (ConfirmDialog — confirm-delete-yes; verified against
 *               frontend/src/pages/Admin/AdminPolicies.jsx, which renders
 *               ConfirmDialog testId="confirm-delete", not window.confirm)
 *
 * Note: name and icon are required; description is optional.
 * The unsaved-changes ConfirmDialog uses testId="confirm-cancel" (NOT confirm-delete).
 */
class AdminPoliciesPage extends AdminBasePage {
  /** @param {import('@playwright/test').Page} page */
  constructor(page) {
    super(page);
    this.entityKey = 'policies';
    this.deleteMode = 'component';
  }

  /**
   * Navigate to the Policies section in the admin sidebar.
   */
  async goto() {
    await this.gotoEntity(this.entityKey);
  }

  /**
   * Open the add form, fill name (required), pick an icon (required),
   * optionally fill description, then save. Returns the create POST response
   * so callers can track the real id instead of re-deriving it from a
   * possibly-paginated row.
   * @param {string} name
   * @param {string} iconKey  — e.g. "ban" (see frontend/src/utils/iconMap.js)
   * @param {{ description?: string }} [opts]
   */
  async createPolicy(name, iconKey, opts = {}) {
    await this.openAddForm();
    await this.fillField('name', name);
    if (opts.description) {
      await this.fillField('description', opts.description);
    }
    await this.pickIcon(iconKey);
    const responsePromise = this.page.waitForResponse(
      (response) => response.url().endsWith('/api/policies') && response.request().method() === 'POST',
    );
    await this.save();
    const response = await responsePromise;
    await this.page.locator('[data-testid="admin-modal"]').waitFor({ state: 'hidden' });
    return response;
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
   * Delete the row identified by id using the ConfirmDialog component.
   * @param {number|string} id
   */
  async deletePolicy(id) {
    await this.deleteRow(id, this.deleteMode);
  }
}

module.exports = AdminPoliciesPage;
