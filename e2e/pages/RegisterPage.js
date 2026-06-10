// @ts-check
const BasePage = require('./BasePage');

class RegisterPage extends BasePage {
  /** @param {import('@playwright/test').Page} page */
  constructor(page) {
    super(page);
    this.firstNameInput = page.locator('input[name="firstName"]');
    this.lastNameInput = page.locator('input[name="lastName"]');
    this.emailInput = page.locator('input[name="email"]');
    this.passwordInput = page.locator('input[name="password"]');
    this.confirmPasswordInput = page.locator('input[name="confirmPassword"]');
    this.submitButton = page.locator('button[type="submit"]');
    this.errorMessage = page.locator('p.error');
  }

  async isLoaded() {
    try {
      await this.waitForVisible(this.submitButton);
      return true;
    } catch {
      return false;
    }
  }

  /**
   * @param {{ firstName: string, lastName: string, email: string, password: string }} user
   */
  async register(user) {
    await this.fill(this.firstNameInput, user.firstName);
    await this.fill(this.lastNameInput, user.lastName);
    await this.fill(this.emailInput, user.email);
    await this.fill(this.passwordInput, user.password);
    await this.fill(this.confirmPasswordInput, user.password);
    await this.click(this.submitButton);
  }
}

module.exports = RegisterPage;
