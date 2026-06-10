// @ts-check
const { test, expect } = require('../fixtures/fixtures');

test.describe('Reservations — authenticated', () => {
  test.skip(!process.env.TEST_USER_EMAIL, 'Set TEST_USER_EMAIL and TEST_USER_PASSWORD in .env');

  test('my-reservations page loads for logged-in user', async ({ page, authUser }) => {
    await page.goto('/my-reservations');
    const heading = page.getByRole('heading');
    await expect(heading.first()).toBeVisible();
  });

  test('unauthenticated user gets redirected from /my-reservations', async ({ page }) => {
    await page.goto('/my-reservations');
    await page.waitForURL(/\/login/);
    expect(page.url()).toContain('/login');
  });
});
