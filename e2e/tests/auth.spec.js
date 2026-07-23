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

    const logoutResponse = page.waitForResponse((response) =>
      response.url().endsWith('/api/auth/logout')
    );
    await page.locator('button.btn-logout').click();
    expect((await logoutResponse).status()).toBe(204);
    const loginLink = page.getByRole('link', { name: 'Iniciar sesión' });
    await expect(loginLink).toBeVisible();
  });

  test('expired token on protected endpoint triggers logout and redirects to /login', async ({ page, loginPage }) => {
    await loginPage.open('/login');
    await loginPage.login(
      process.env.TEST_USER_EMAIL,
      process.env.TEST_USER_PASSWORD,
    );
    await page.waitForURL('/');

    await page.route('**/api/favorites**', (route) =>
      route.fulfill({
        status: 401,
        contentType: 'application/json',
        body: JSON.stringify({ message: 'Unauthorized' }),
      })
    );

    await page.reload();

    await page.waitForURL('**/login', { timeout: 5000 });

    const token = await page.evaluate(() => localStorage.getItem('token'));
    expect(token).toBeNull();
  });

  test('register redirects to home on success', async ({ page, registerPage }) => {
    const uniqueEmail = `e2e_${Date.now()}@test.com`;
    await registerPage.open('/register');
    const csrfResponse = page.waitForResponse((response) =>
      response.url().endsWith('/api/auth/csrf')
    );
    await registerPage.register({
      firstName: 'Test',
      lastName: 'E2E',
      email: uniqueEmail,
      password: 'Test1234',
    });
    expect((await csrfResponse).status()).toBe(204);
    await page.waitForURL('/');

    const logoutResponse = page.waitForResponse((response) =>
      response.url().endsWith('/api/auth/logout')
    );
    await page.locator('button.btn-logout').click();
    expect((await logoutResponse).status()).toBe(204);
    await expect(page.getByRole('link', { name: 'Iniciar sesión' })).toBeVisible();
  });
});
