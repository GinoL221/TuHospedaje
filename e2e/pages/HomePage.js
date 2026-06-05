// @ts-check
const BasePage = require('./BasePage');

class HomePage extends BasePage {
  /** @param {import('@playwright/test').Page} page */
  constructor(page) {
    super(page);
    this.cityInput = page.getByPlaceholder('Ciudad');
    this.searchButton = page.locator('button.btn-search');
    this.lodgingCards = page.locator('.product-card');
  }

  async isLoaded() {
    try {
      await this.waitForVisible(this.searchButton);
      return true;
    } catch {
      return false;
    }
  }

  /** @param {string} city */
  async searchByCity(city) {
    await this.fill(this.cityInput, city);
    await this.click(this.searchButton);
  }
}

module.exports = HomePage;
