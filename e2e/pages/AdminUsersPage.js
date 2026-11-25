// @ts-check
const AdminBasePage = require('./AdminBasePage');

/**
 * AdminUsersPage — page object for the Admin › Users section.
 *
 * Entity key : "users"
 * Operations : read user list, toggle role via ConfirmDialog
 * No create, no edit, no delete — read-only except for role toggle.
 *
 * Role-toggle confirmation uses the ConfirmDialog component
 * (testId="confirm-role-toggle").
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
   * Click the role-toggle button and confirm in the ConfirmDialog.
   * @param {number|string} id
   */
  async toggleRole(id) {
    await this.getRoleBtn(id).click();
    const yesBtn = this.page.locator('[data-testid="confirm-role-toggle-yes"]');
    await yesBtn.waitFor({ state: 'visible' });
    await yesBtn.click();
    // Give React a tick to process the PUT response and re-render.
    await this.page.waitForTimeout(400);
  }
}

module.exports = AdminUsersPage;
