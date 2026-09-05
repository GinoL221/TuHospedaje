// @ts-check
const { test, expect } = require('../fixtures/fixtures');

/**
 * @param {import('@playwright/test').Page} page
 * @param {import('@playwright/test').Locator} locator
 */
async function expectContainedInViewport(page, locator) {
  const box = await locator.boundingBox();
  expect(box).not.toBeNull();

  const viewportWidth = page.viewportSize()?.width ?? 0;
  expect(box.x).toBeGreaterThanOrEqual(0);
  expect(box.x + box.width).toBeLessThanOrEqual(viewportWidth + 1);
}

/** @param {import('@playwright/test').Page} page */
async function expectNoHorizontalOverflow(page) {
  const documentSize = await page.evaluate(() => ({
    clientWidth: document.documentElement.clientWidth,
    scrollWidth: document.documentElement.scrollWidth,
  }));

  expect(documentSize.scrollWidth).toBeLessThanOrEqual(documentSize.clientWidth);
}

/**
 * @param {import('@playwright/test').Page} page
 * @param {string} cardSelector
 * @param {import('@playwright/test').Locator[]} controls
 */
async function expectAuthLayout(page, cardSelector, controls) {
  const card = page.locator(cardSelector);
  await expect(card).toBeVisible();
  await expectNoHorizontalOverflow(page);
  await expectContainedInViewport(page, card);

  for (const control of controls) {
    await control.scrollIntoViewIfNeeded();
    await expect(control).toBeVisible();
    await expectContainedInViewport(page, control);
  }

  await expectNoHorizontalOverflow(page);
}

test.describe('Mobile auth containment at 390px', () => {
  test('contains the login card and keeps its controls reachable', async ({ page, loginPage }) => {
    await loginPage.open('/login');

    await expectAuthLayout(page, '.login-box', [
      loginPage.emailInput,
      loginPage.passwordInput,
      loginPage.submitButton,
    ]);
  });

  test('contains the registration card and keeps its controls reachable', async ({ page, registerPage }) => {
    await registerPage.open('/register');

    await expectAuthLayout(page, '.register-box', [
      registerPage.firstNameInput,
      registerPage.lastNameInput,
      registerPage.emailInput,
      registerPage.passwordInput,
      registerPage.confirmPasswordInput,
      registerPage.submitButton,
    ]);
  });
});

test.describe('Mobile auth containment at 320px', () => {
  test.use({ viewport: { width: 320, height: 844 } });

  test('contains the login card and keeps its controls reachable', async ({ page, loginPage }) => {
    await loginPage.open('/login');

    await expectAuthLayout(page, '.login-box', [
      loginPage.emailInput,
      loginPage.passwordInput,
      loginPage.submitButton,
    ]);
  });

  test('contains the registration card and keeps its controls reachable', async ({ page, registerPage }) => {
    await registerPage.open('/register');

    await expectAuthLayout(page, '.register-box', [
      registerPage.firstNameInput,
      registerPage.lastNameInput,
      registerPage.emailInput,
      registerPage.passwordInput,
      registerPage.confirmPasswordInput,
      registerPage.submitButton,
    ]);
  });
});
