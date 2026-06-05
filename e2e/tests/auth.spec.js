// @ts-check
const { test, expect } = require('../fixtures/fixtures');

test.describe('Auth flow', () => {
  test.skip(!process.env.TEST_USER_EMAIL, 'Set TEST_USER_EMAIL and TEST_USER_PASSWORD in .env');

  test('login with valid credentials shows user name in header', async ({ page, loginPage }) => {
    await loginPage.open('/login');
    await loginPage.login(
      process.env.TEST_USER_EMAIL,
      process.env.TEST_USER_PASSWORD,
    );
    await page.waitForURL('/');
    const userName = page.locator('nav').getByText(process.env.TEST_USER_NAME || 'Admin');
    await expect(userName).toBeVisible();
  });

  test('login with wrong password shows error', async ({ loginPage }) => {
    await loginPage.open('/login');
    await loginPage.login('noexiste@test.com', 'WrongPass1');
    const error = await loginPage.getErrorText();
    expect(error).not.toBeNull();
  });

  test('logout after login redirects to home with login link', async ({ page, loginPage }) => {
    await loginPage.open('/login');
    await loginPage.login(
      process.env.TEST_USER_EMAIL,
      process.env.TEST_USER_PASSWORD,
    );
    await page.waitForURL('/');

    await page.locator('button.btn-logout').click();
    const loginLink = page.getByRole('link', { name: 'Iniciar sesión' });
    await expect(loginLink).toBeVisible();
  });

  test('register redirects to home on success', async ({ page, registerPage }) => {
    const uniqueEmail = `e2e_${Date.now()}@test.com`;
    await registerPage.open('/register');
    await registerPage.register({
      firstName: 'Test',
      lastName: 'E2E',
      email: uniqueEmail,
      password: 'Test1234',
    });
    await page.waitForURL('/');
    const url = page.url();
    expect(url).toContain('/');
  });
});
