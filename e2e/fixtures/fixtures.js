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

const DEFAULT_ADMIN_EMAIL = 'admin@tuhospedaje.com';
const DEFAULT_ADMIN_PASSWORD = 'Admin1';
const SHARED_SESSION_LOGOUT_PATH = '/api/auth/logout';

/**
 * Resolve admin credentials from environment variables, falling back to the
 * seeded development defaults.
 *
 * @param {NodeJS.ProcessEnv} [env=process.env]
 * @returns {{ email: string, password: string }}
 */
function resolveAdminCredentials(env = process.env) {
  return {
    email: env.TEST_ADMIN_EMAIL || DEFAULT_ADMIN_EMAIL,
    password: env.TEST_ADMIN_PASSWORD || DEFAULT_ADMIN_PASSWORD,
  };
}

/**
 * Resolve regular-user credentials from environment variables.
 *
 * @param {NodeJS.ProcessEnv} [env=process.env]
 * @returns {{ email: string, password: string, name: string }}
 */
function resolveUserCredentials(env = process.env) {
  return {
    email: env.TEST_USER_EMAIL || '',
    password: env.TEST_USER_PASSWORD || '',
    name: env.TEST_USER_NAME || '',
  };
}

/**
 * Apply a saved Playwright storageState (cookies + optional origin localStorage)
 * onto the test page's browser context without mutating project-level defaults.
 *
 * Shared refresh-token family invariant:
 * - In development, `ACCESS_TOKEN` uses an 8h TTL and `auth.setup.js` creates
 *   one shared refresh-token family per role (`authUser`, `adminUser`).
 * - Because every test using `authUser` or `adminUser` shares that family,
 *   calling `POST /api/auth/logout` or rotating tokens inside a shared-session
 *   fixture revokes the session for all remaining tests.
 * - Logout and token-rotation specs must use an unauthenticated bare `page`
 *   instead of `authUser` / `adminUser`.
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
 * Attach a lightweight request guard on `page` that records any
 * `POST /api/auth/logout` sent through a shared-session fixture.
 *
 * Call the returned teardown function after `use(...)` to detach the listener
 * and fail the test cleanly if logout was called, protecting the shared
 * refresh-token family for subsequent tests.
 *
 * @param {import('@playwright/test').Page} page
 * @param {string} roleLabel
 * @returns {() => void}
 */
function guardSharedSessionLogout(page, roleLabel) {
  /** @type {string[]} */
  const logoutRequests = [];

  /** @param {import('@playwright/test').Request} request */
  const onRequest = (request) => {
    if (request.method() !== 'POST') {
      return;
    }
    try {
      const { pathname } = new URL(request.url());
      if (pathname.endsWith(SHARED_SESSION_LOGOUT_PATH)) {
        logoutRequests.push(`${request.method()} ${pathname}`);
      }
    } catch {
      if (request.url().includes(SHARED_SESSION_LOGOUT_PATH)) {
        logoutRequests.push(`${request.method()} ${request.url()}`);
      }
    }
  };

  page.on('request', onRequest);

  return () => {
    page.off('request', onRequest);
    if (logoutRequests.length > 0) {
      throw new Error(
        `[${roleLabel}] Test issued ${logoutRequests[0]} using a shared storageState session. ` +
          'Calling logout revokes the shared refresh-token family for all remaining tests; ' +
          'logout and token-rotation specs must use bare `page` without `authUser`/`adminUser`.',
      );
    }
  };
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
  /**
   * Regular authenticated user fixture backed by `USER_STORAGE_STATE`, landed on `'/'`.
   *
   * Shared refresh-token family invariant: reuses the single dev session created by
   * `auth.setup.js` (8h `ACCESS_TOKEN` TTL). Logout/rotation tests must use bare
   * unauthenticated `page`; issuing `POST /api/auth/logout` fails in teardown.
   */
  authUser: async ({ page }, use) => {
    const { email, password, name } = resolveUserCredentials();
    const assertNoSharedLogout = guardSharedSessionLogout(page, 'authUser');

    try {
      await applyStorageState(page, USER_STORAGE_STATE, 'authUser');
      await page.goto('/');

      await use({ email, password, name });
    } finally {
      assertNoSharedLogout();
    }
  },
  /**
   * Authenticated admin fixture backed by `ADMIN_STORAGE_STATE`, landed on `'/admin'`
   * with the admin shell visible.
   *
   * Shared refresh-token family invariant: reuses the single dev session created by
   * `auth.setup.js` (8h `ACCESS_TOKEN` TTL). Logout/rotation tests must use bare
   * unauthenticated `page`; issuing `POST /api/auth/logout` fails in teardown.
   */
  adminUser: async ({ page }, use) => {
    const { email, password } = resolveAdminCredentials();
    const assertNoSharedLogout = guardSharedSessionLogout(page, 'adminUser');

    try {
      await applyStorageState(page, ADMIN_STORAGE_STATE, 'adminUser');
      await runAdminStage(page, 'admin-shell', async () => {
        await page.goto('/admin');
        await expect(page.locator('[data-testid="admin-nav-dashboard"]')).toBeVisible();
      });

      await use({ page, email, password });
    } finally {
      assertNoSharedLogout();
    }
  },
}));

module.exports = {
  test,
  expect,
  AUTH_DIR,
  ADMIN_STORAGE_STATE,
  USER_STORAGE_STATE,
  DEFAULT_ADMIN_EMAIL,
  DEFAULT_ADMIN_PASSWORD,
  resolveAdminCredentials,
  resolveUserCredentials,
};
