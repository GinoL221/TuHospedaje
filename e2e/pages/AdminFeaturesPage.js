// @ts-check
const AdminBasePage = require('./AdminBasePage');

/**
 * AdminFeaturesPage — page object for the Admin › Features section.
 *
 * Entity key : "features"
 * Form fields : name (text input), icon (IconPicker — NOT fillField)
 * Delete mode : "component" (ConfirmDialog — confirm-delete-yes; verified against
 *               frontend/src/pages/Admin/AdminFeatures.jsx, which renders
 *               ConfirmDialog testId="confirm-delete", not window.confirm)
 */
class AdminFeaturesPage extends AdminBasePage {
  /** @param {import('@playwright/test').Page} page */
  constructor(page) {
    super(page);
    this.entityKey = 'features';
    this.deleteMode = 'component';
  }

  /**
   * Navigate to the Features section in the admin sidebar.
   */
  async goto() {
    await this.gotoEntity(this.entityKey);
  }

  /**
   * Open the add form, fill name and pick an icon, then save. Returns the
   * create POST response so callers can track the real id instead of
   * re-deriving it from a possibly-paginated row.
   * @param {string} name
   * @param {string} iconKey  — e.g. "wifi" (see frontend/src/utils/iconMap.js)
   */
  async createFeature(name, iconKey) {
    await this.openAddForm();
    await this.fillField('name', name);
    await this.pickIcon(iconKey);
    const responsePromise = this.page.waitForResponse(
      (response) => response.url().endsWith('/api/features') && response.request().method() === 'POST',
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
  async editFeatureName(id, newName) {
    await this.editRow(id);
    await this.fillField('name', newName);
    await this.save();
  }

  /**
   * Delete the row identified by id using the ConfirmDialog component.
   * @param {number|string} id
   */
  async deleteFeature(id) {
    await this.deleteRow(id, this.deleteMode);
  }
}

module.exports = AdminFeaturesPage;
