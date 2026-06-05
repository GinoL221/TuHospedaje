// @ts-check
const { test, expect } = require('../fixtures/fixtures');

test.describe('Smoke — Home', () => {
  test('home page loads with search form', async ({ homePage }) => {
    await homePage.open('/');
    const loaded = await homePage.isLoaded();
    expect(loaded).toBe(true);
  });

  test('login page loads', async ({ loginPage }) => {
    await loginPage.open('/login');
    const loaded = await loginPage.isLoaded();
    expect(loaded).toBe(true);
  });

  test('register page loads', async ({ registerPage }) => {
    await registerPage.open('/register');
    const loaded = await registerPage.isLoaded();
    expect(loaded).toBe(true);
  });
});
