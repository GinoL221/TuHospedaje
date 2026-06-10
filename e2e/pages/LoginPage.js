// @ts-check
const BasePage = require('./BasePage');

class LoginPage extends BasePage {
  /** @param {import('@playwright/test').Page} page */
  constructor(page) {
    super(page);
    this.emailInput = page.locator('input[name="email"]');
    this.passwordInput = page.locator('input[name="password"]');
    this.submitButton = page.locator('button[type="submit"]');
    this.errorMessage = page.locator('p.error');
  }

  async isLoaded() {
    try {
      await this.waitForVisible(this.submitButton);
      return (await this.getText(this.submitButton)).trim().length > 0;
    } catch {
      return false;
    }
  }

  /**
   * @param {string} email
   * @param {string} password
   */
  async login(email, password) {
    await this.fill(this.emailInput, email);
    await this.fill(this.passwordInput, password);
    await this.click(this.submitButton);
  }

  async getErrorText() {
    try {
      await this.waitForVisible(this.errorMessage);
      return await this.getText(this.errorMessage);
    } catch {
      return null;
    }
  }
}

module.exports = LoginPage;
