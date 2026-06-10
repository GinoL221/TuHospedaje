// @ts-check
const { test, expect } = require('../fixtures/fixtures');

// Images from Unsplash are masked to avoid flakiness from external CDN variance.
const maskImages = (page) => [page.locator('img')];

test.describe('Visual regression', () => {
  test('home page', async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveScreenshot('home.png', {
      mask: maskImages(page),
    });
  });

  test('login page', async ({ page }) => {
    await page.goto('/login');
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveScreenshot('login.png');
  });

  test('register page', async ({ page }) => {
    await page.goto('/register');
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveScreenshot('register.png');
  });

  test('search results page', async ({ page }) => {
    await page.goto('/search?city=Buenos+Aires');
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveScreenshot('search-results.png', {
      mask: maskImages(page),
    });
  });

  test('lodging detail page', async ({ page }) => {
    await page.goto('/lodgings/1');
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveScreenshot('lodging-detail.png', {
      mask: maskImages(page),
    });
  });

  test.skip(!process.env.TEST_USER_EMAIL, 'Set TEST_USER_EMAIL and TEST_USER_PASSWORD in .env');

  test('my reservations page (authenticated)', async ({ page, authUser }) => {
    await page.goto('/my-reservations');
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveScreenshot('my-reservations.png', {
      mask: maskImages(page),
    });
  });
});
