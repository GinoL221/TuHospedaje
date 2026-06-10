// @ts-check

class BasePage {
  /** @param {import('@playwright/test').Page} page */
  constructor(page) {
    this.page = page;
  }

  /** @param {string} path */
  async open(path) {
    await this.page.goto(path);
  }

  /** @param {import('@playwright/test').Locator} locator */
  async click(locator) {
    await locator.click();
  }

  /**
   * @param {import('@playwright/test').Locator} locator
   * @param {string} value
   */
  async fill(locator, value) {
    await locator.fill(value);
  }

  /** @param {import('@playwright/test').Locator} locator */
  async getText(locator) {
    return await locator.innerText();
  }

  /** @param {import('@playwright/test').Locator} locator */
  async waitForVisible(locator) {
    await locator.waitFor({ state: 'visible' });
  }
}

module.exports = BasePage;
