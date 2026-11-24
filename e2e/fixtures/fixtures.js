// @ts-check
const fs = require('node:fs');
const path = require('node:path');
const { test: base, expect } = require('@playwright/test');
const HomePage = require('../pages/HomePage');
const LoginPage = require('../pages/LoginPage');
const RegisterPage = require('../pages/RegisterPage');

const AUTH_DIR = path.resolve(__dirname, '../.auth');
const ADMIN_STORAGE_STATE = path.join(AUTH_DIR, 'admin.json');
const USER_STORAGE_STATE = path.join(AUTH_DIR, 'user.json');

/**
 * Apply a saved Playwright storageState (cookies + optional origin localStorage)
 * onto the test page's browser context without mutating project-level defaults.
 *
 * @param {import('@playwright/test').Page} page
 * @param {string} statePath
 * @param {string} roleLabel
 */
async function applyStorageState(page, statePath, roleLabel) {
  if (!fs.existsSync(statePath)) {
    throw new Error(
      `[${roleLabel}] Storage state file not found at ${statePath}. Run the Playwright "setup" project first.`,
    );
  }

  const raw = fs.readFileSync(statePath, 'utf-8');
  let state;
  try {
    state = JSON.parse(raw);
  } catch (err) {
    const reason = err instanceof Error ? err.message : String(err);
    throw new Error(
      `[${roleLabel}] Invalid storage state JSON at ${statePath}: ${reason}. Re-run the Playwright "setup" project.`,
    );
  }

  if (Array.isArray(state.cookies) && state.cookies.length > 0) {
    await page.context().addCookies(state.cookies);
  }

  if (Array.isArray(state.origins)) {
    for (const originEntry of state.origins) {
      if (
        originEntry &&
        typeof originEntry.origin === 'string' &&
        Array.isArray(originEntry.localStorage) &&
        originEntry.localStorage.length > 0
      ) {
        await page.context().addInitScript(
          ({ targetOrigin, entries }) => {
            if (window.location.origin === targetOrigin) {
              for (const { name, value } of entries) {
                window.localStorage.setItem(name, value);
              }
            }
          },
          { targetOrigin: originEntry.origin, entries: originEntry.localStorage },
        );
      }
    }
  }
}

/**
 * @param {import('@playwright/test').Page} page
 * @param {string} stage
 * @param {() => Promise<void>} action
 */
async function runAdminStage(page, stage, action) {
  try {
    await action();
  } catch {
    const currentUrl = new URL(page.url());
    currentUrl.username = '';
    currentUrl.password = '';
    currentUrl.search = '';
    currentUrl.hash = '';
    throw new Error(
      `[adminUser] stage=${stage} currentUrl=${currentUrl.toString()}`,
    );
  }
}

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

    await applyStorageState(page, USER_STORAGE_STATE, 'authUser');

    await use({ email, password, name });
  },
  adminUser: async ({ page }, use) => {
    const email = process.env.TEST_ADMIN_EMAIL || 'admin@tuhospedaje.com';
    const password = process.env.TEST_ADMIN_PASSWORD || 'Admin1';

    await applyStorageState(page, ADMIN_STORAGE_STATE, 'adminUser');
    await runAdminStage(page, 'admin-shell', async () => {
      await page.goto('/admin');
      await expect(page.locator('[data-testid="admin-nav-dashboard"]')).toBeVisible();
    });

    await use({ page, email, password });
  },
}));

module.exports = {
  test,
  expect,
  AUTH_DIR,
  ADMIN_STORAGE_STATE,
  USER_STORAGE_STATE,
};
