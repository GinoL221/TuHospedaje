// @ts-check
const { test, expect } = require('../fixtures/fixtures');
const { expectNoA11yViolations } = require('../fixtures/a11y');

test.describe('Accessibility (WCAG 2.0/2.1 A/AA)', () => {
  test.describe('Public routes', () => {
    test('Home (/) has no WCAG A/AA violations', async ({ page, homePage }) => {
      await homePage.open('/');
      await expect(homePage.searchButton).toBeVisible();
      await expect(page.locator('main article').first()).toBeVisible();

      await expectNoA11yViolations(page, 'Home (/)');
    });

    test('Lodging detail (/lodgings/1) has no WCAG A/AA violations', async ({ page }) => {
      await page.goto('/lodgings/1');
      await expect(page.locator('h1').first()).toBeVisible();

      await expectNoA11yViolations(page, 'Lodging detail (/lodgings/1)');
    });
  });

  test.describe('Auth routes', () => {
    test('Login (/login) has no WCAG A/AA violations', async ({ page, loginPage }) => {
      await loginPage.open('/login');
      await expect(loginPage.emailInput).toBeVisible();
      await expect(loginPage.submitButton).toBeVisible();

      await expectNoA11yViolations(page, 'Login (/login)');
    });

    test('Register (/register) has no WCAG A/AA violations', async ({ page, registerPage }) => {
      await registerPage.open('/register');
      await expect(page.locator('form input').first()).toBeVisible();
      await expect(registerPage.submitButton).toBeVisible();

      await expectNoA11yViolations(page, 'Register (/register)');
    });
  });

  test.describe('Admin routes', () => {
    test('Admin dashboard (/admin) has no WCAG A/AA violations', async ({ adminUser }) => {
      const { page } = adminUser;
      await expect(page.locator('[data-testid="admin-nav-dashboard"]')).toBeVisible();
      await expect(page.locator('main, [role="main"], h1, h2').first()).toBeVisible();

      await expectNoA11yViolations(page, 'Admin dashboard (/admin)');
    });
  });
});
