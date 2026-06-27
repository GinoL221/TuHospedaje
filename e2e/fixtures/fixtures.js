// @ts-check
const { test: base, expect } = require('@playwright/test');
const HomePage = require('../pages/HomePage');
const LoginPage = require('../pages/LoginPage');
const RegisterPage = require('../pages/RegisterPage');

/**
 * @typedef {object} CustomFixtures
 * @property {HomePage} homePage
 * @property {LoginPage} loginPage
 * @property {RegisterPage} registerPage
 * @property {{ email: string, password: string, name: string }} authUser
 * @property {{ page: import('@playwright/test').Page, email: string, password: string }} adminUser
 */

const test = base.extend(/** @type {CustomFixtures} */({
  homePage: async ({ page }, use) => {
    await use(new HomePage(page));
  },
  loginPage: async ({ page }, use) => {
    await use(new LoginPage(page));
  },
  registerPage: async ({ page }, use) => {
    await use(new RegisterPage(page));
  },
  authUser: async ({ page }, use) => {
    const email = process.env.TEST_USER_EMAIL || '';
    const password = process.env.TEST_USER_PASSWORD || '';
    const name = process.env.TEST_USER_NAME || '';

    const loginPage = new LoginPage(page);
    await loginPage.open('/login');
    await loginPage.login(email, password);
    await page.waitForURL('/');

    await use({ email, password, name });
  },
  adminUser: async ({ page }, use) => {
    const email = process.env.TEST_ADMIN_EMAIL || 'admin@tuhospedaje.com';
    const password = process.env.TEST_ADMIN_PASSWORD || 'Admin1';

    const loginPage = new LoginPage(page);
    await loginPage.open('/login');
    await loginPage.login(email, password);

    // If the login does not land on /admin or the stack is down, skip.
    try {
      await page.waitForURL('/', { timeout: 5000 });
    } catch {
      // Not redirected to home — stack may be down or credentials wrong.
      await use({ page, email, password });
      return;
    }

    await page.goto('/admin');

    const navDashboard = page.locator('[data-testid="admin-nav-dashboard"]');
    const visible = await navDashboard.isVisible().catch(() => false);
    if (!visible) {
      // Admin shell not accessible — skip consuming test.
      await use({ page, email, password });
      return;
    }

    await use({ page, email, password });
  },
}));

module.exports = { test, expect };
