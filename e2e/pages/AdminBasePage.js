// @ts-check
const BasePage = require('./BasePage');

/**
 * AdminBasePage — shared verbs for all admin CRUD sections.
 *
 * Delete confirmation comes in two flavours:
 *   "dialog"    — native window.confirm (Features, Policies, Users)
 *   "component" — ConfirmDialog DOM component (Categories, Lodgings)
 *
 * Entity key must match the admin-nav-{key} data-testid value.
 */
class AdminBasePage extends BasePage {
  /** @param {import('@playwright/test').Page} page */
  constructor(page) {
    super(page);
  }

  // ---------------------------------------------------------------------------
  // Navigation
  // ---------------------------------------------------------------------------

  /**
   * Click the sidebar nav button for the given entity key and wait until the
   * add-button for that section is visible (or at least the nav item is active).
   * @param {string} key
   */
  async gotoEntity(key) {
    await this.page.locator(`[data-testid="admin-nav-${key}"]`).click();
    // Give React a tick to render the new tab content.
    await this.page.waitForTimeout(300);
  }

  // ---------------------------------------------------------------------------
  // Modal / form
  // ---------------------------------------------------------------------------

  /** Click the FAB and wait for the modal to appear. */
  async openAddForm() {
    await this.page.locator('[data-testid="admin-add-btn"]').click();
    await this.page.locator('[data-testid="admin-modal"]').waitFor({ state: 'visible' });
  }

  /**
   * Fill a form field identified by its data-testid (field-{name}).
   * @param {string} name  — field name, e.g. "name", "email", "address"
   * @param {string} value
   */
  async fillField(name, value) {
    const locator = this.page.locator(`[data-testid="field-${name}"]`);
    await locator.fill(value);
  }

  /**
   * Open the IconPicker, optionally search for a key, then click the item.
   * The picker uses createPortal to document.body — locate items at page root.
   * @param {string} key  — icon key, e.g. "fa-solid fa-wifi"
   */
  async pickIcon(key) {
    await this.page.locator('[data-testid="icon-picker-trigger"]').click();
    const search = this.page.locator('[data-testid="icon-picker-search"]');
    await search.waitFor({ state: 'visible' });
    // Clear existing search and type the key to narrow the list.
    await search.fill(key);
    // Items render in document.body via portal — use page root locator.
    const item = this.page.locator(`[data-testid="icon-picker-item-${key}"]`);
    await item.waitFor({ state: 'visible' });
    await item.click();
  }

  /** Click the save button inside the modal. */
  async save() {
    await this.page.locator('[data-testid="admin-save-btn"]').click();
  }

  /** Click the cancel button inside the modal. */
  async cancel() {
    await this.page.locator('[data-testid="admin-cancel-btn"]').click();
  }

  // ---------------------------------------------------------------------------
  // Table
  // ---------------------------------------------------------------------------

  /**
   * Return the row locator for a given entity id.
   * @param {number|string} id
   */
  findRow(id) {
    return this.page.locator(`[data-testid="row-${id}"]`);
  }

  /**
   * Return the first row that contains the given text anywhere inside it.
   * @param {string} text
   */
  findRowByText(text) {
    return this.page.locator('tbody tr').filter({ hasText: text });
  }

  /**
   * Find a row by id, navigating to the last table page if it isn't on the
   * current page. Tables paginate ascending by id, so a freshly created row
   * is not necessarily on page 1 once earlier rows (seed data or leftover
   * test rows) fill it up.
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
   * Delete tracked rows through the admin UI by id. Cleanup failures fail the
   * test instead of being swallowed, so a broken delete flow is never hidden.
   * @param {Array<number|string>} ids
   * @param {'dialog'|'component'} mode
   */
  async cleanupCreatedRows(ids, mode) {
    for (const id of ids) {
      const row = await this.findCreatedRow(id);
      await this.deleteRow(id, mode);
      await row.waitFor({ state: 'detached' });
    }
  }

  /**
   * Click the edit button in the row identified by id.
   * @param {number|string} id
   */
  async editRow(id) {
    const row = this.findRow(id);
    await row.locator('[data-testid="row-edit-btn"]').click();
    await this.page.locator('[data-testid="admin-modal"]').waitFor({ state: 'visible' });
  }

  /**
   * Click the delete button in the row identified by id, then confirm.
   * @param {number|string} id
   * @param {'dialog'|'component'} mode
   *   "dialog"    — register page.on('dialog') BEFORE click
   *   "component" — click confirm-delete-yes in the ConfirmDialog component
   */
  async deleteRow(id, mode) {
    const row = this.findRow(id);
    if (mode === 'dialog') {
      // Register the handler BEFORE the click that triggers the native dialog.
      const dialogHandler = (/** @type {import('@playwright/test').Dialog} */ dialog) => dialog.accept();
      this.page.once('dialog', dialogHandler);
      await row.locator('[data-testid="row-delete-btn"]').click();
    } else {
      await row.locator('[data-testid="row-delete-btn"]').click();
      await this.confirmDelete();
    }
  }

  /**
   * Confirm deletion via the ConfirmDialog component (mode="component").
   * Call this only when the ConfirmDialog is visible on screen.
   */
  async confirmDelete() {
    const yesBtn = this.page.locator('[data-testid="confirm-delete-yes"]');
    await yesBtn.waitFor({ state: 'visible' });
    await yesBtn.click();
  }

  // ---------------------------------------------------------------------------
  // Validation
  // ---------------------------------------------------------------------------

  /**
   * Assert that the field-error node for the given field name is visible.
   * @param {string} field  — e.g. "name", "email"
   */
  async expectFieldError(field) {
    await this.page.locator(`[data-testid="error-${field}"]`).waitFor({ state: 'visible' });
  }
}

module.exports = AdminBasePage;
