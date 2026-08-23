// @ts-check
const AdminBasePage = require('./AdminBasePage');

/**
 * AdminCategoriesPage — page object for the Admin › Categories section.
 *
 * Entity key : "categories"
 * Form fields : name (text input), description (textarea), icon (IconPicker — NOT fillField),
 *               image-url (text input, testId field-image-url — representative image, US-21.1)
 * Delete mode : "component" (ConfirmDialog — confirm-delete-yes)
 *
 * Note: name and a valid HTTPS representative image are required on create;
 * description and icon are optional. The delete ConfirmDialog uses
 * testId="confirm-delete". The unsaved-changes ConfirmDialog uses
 * testId="confirm-cancel".
 */
const DEFAULT_IMAGE_URL = 'https://img.example.com/e2e-category-fixture.jpg';

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
   * pick an icon, then save. Returns the create POST response so callers can
   * track the real id instead of re-deriving it from a possibly-paginated row.
   * @param {string} name
   * @param {{ description?: string; iconKey?: string; imageUrl?: string }} [opts]
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
    // Deterministic local fixture URL — never a real image-hosting
    // credential or live remote fetch (see US-21.1-S1).
    await this.fillField('image-url', opts.imageUrl || DEFAULT_IMAGE_URL);
    const responsePromise = this.page.waitForResponse(
      (response) => response.url().endsWith('/api/categories') && response.request().method() === 'POST',
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
