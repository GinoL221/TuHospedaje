// @ts-check
const fs = require('node:fs');
const { test: setup, expect } = require('@playwright/test');
const LoginPage = require('../pages/LoginPage');
const {
  AUTH_DIR,
  ADMIN_STORAGE_STATE,
  USER_STORAGE_STATE,
} = require('../fixtures/fixtures');

fs.mkdirSync(AUTH_DIR, { recursive: true });

setup('authenticate admin', async ({ page }) => {
  const email = process.env.TEST_ADMIN_EMAIL || 'admin@tuhospedaje.com';
  const password = process.env.TEST_ADMIN_PASSWORD || 'Admin1';

  const loginPage = new LoginPage(page);
  await loginPage.open('/login');
  await loginPage.login(email, password);

  await expect(page).not.toHaveURL(/\/login$/);
  await page.goto('/admin');
  await expect(page.locator('[data-testid="admin-nav-dashboard"]')).toBeVisible();

  await page.context().storageState({ path: ADMIN_STORAGE_STATE });
});

setup('authenticate user', async ({ page }) => {
  const email = process.env.TEST_USER_EMAIL || '';
  const password = process.env.TEST_USER_PASSWORD || '';

  setup.skip(
    !email || !password,
    'Set TEST_USER_EMAIL and TEST_USER_PASSWORD to generate user storageState',
  );

  const loginPage = new LoginPage(page);
  await loginPage.open('/login');
  await loginPage.login(email, password);

  await expect(page).not.toHaveURL(/\/login$/);
  await page.waitForURL('/');

  const cookies = await page.context().cookies();
  expect(
    cookies.some((cookie) => cookie.name === 'ACCESS_TOKEN'),
    'Expected ACCESS_TOKEN cookie after user login',
  ).toBe(true);

  await page.context().storageState({ path: USER_STORAGE_STATE });
});
