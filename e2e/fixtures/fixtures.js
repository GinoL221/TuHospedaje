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
}));

module.exports = { test, expect };
