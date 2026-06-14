// @ts-check
const AdminBasePage = require('./AdminBasePage');

/**
 * AdminUsersPage — page object for the Admin › Users section.
 *
 * Entity key : "users"
 * Operations : read user list, toggle role via window.confirm
 * No create, no edit, no delete — read-only except for role toggle.
 *
 * Role-toggle confirmation is a native window.confirm dialog.
 * The handler MUST be registered via page.once('dialog') before the click.
 */
class AdminUsersPage extends AdminBasePage {
  /** @param {import('@playwright/test').Page} page */
  constructor(page) {
    super(page);
    this.entityKey = 'users';
  }

  /**
   * Navigate to the Users section in the admin sidebar.
   */
  async goto() {
    await this.gotoEntity(this.entityKey);
  }

  /**
   * Return the role-toggle button locator for a given user row id.
   * @param {number|string} id
   */
  getRoleBtn(id) {
    return this.findRow(id).locator('[data-testid="row-role-btn"]');
  }

  /**
   * Accept the window.confirm dialog and click the role-toggle button.
   * The dialog handler is registered BEFORE the click so it is in place
   * when the native dialog fires.
   * @param {number|string} id
   */
  async toggleRole(id) {
    this.page.once('dialog', (dialog) => dialog.accept());
    await this.getRoleBtn(id).click();
    // Give React a tick to process the PUT response and re-render.
    await this.page.waitForTimeout(400);
  }
}

module.exports = AdminUsersPage;
