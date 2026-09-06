// @ts-check
const { test, expect } = require('../fixtures/fixtures');

const LODGING_ID = 6;

/** @param {import('@playwright/test').Locator} locator */
async function expectTouchTarget(locator) {
  const box = await locator.boundingBox();
  expect(box).not.toBeNull();
  expect(box.width).toBeGreaterThanOrEqual(44);
  expect(box.height).toBeGreaterThanOrEqual(44);
}

/**
 * @param {import('@playwright/test').Page} page
 * @param {import('@playwright/test').Locator} locator
 */
async function expectContainedInViewport(page, locator) {
  const box = await locator.boundingBox();
  expect(box).not.toBeNull();

  const viewport = page.viewportSize();
  expect(viewport).not.toBeNull();
  expect(box.x).toBeGreaterThanOrEqual(-1);
  expect(box.y).toBeGreaterThanOrEqual(-1);
  expect(box.x + box.width).toBeLessThanOrEqual(viewport.width + 1);
  expect(box.y + box.height).toBeLessThanOrEqual(viewport.height + 1);
}

/** @param {import('@playwright/test').Page} page */
async function expectNoHorizontalOverflow(page) {
  const documentSize = await page.evaluate(() => ({
    clientWidth: document.documentElement.clientWidth,
    scrollWidth: document.documentElement.scrollWidth,
  }));

  expect(documentSize.scrollWidth).toBeLessThanOrEqual(documentSize.clientWidth);
}

/** @param {import('@playwright/test').Page} page */
async function interceptAvailability(page) {
  await page.route(`**/lodgings/${LODGING_ID}/availability**`, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ occupiedRanges: [] }),
    });
  });
}

/** @param {import('@playwright/test').Page} page */
async function exerciseProductDetail(page) {
  await interceptAvailability(page);
  await page.goto(`/lodgings/${LODGING_ID}`);

  await expect(page.locator('.product-detail h1')).toBeVisible();
  await expect(page.locator('#product-check-in')).toBeEnabled();
  await expectNoHorizontalOverflow(page);

  const checkIn = page.locator('#product-check-in');
  const checkOut = page.locator('#product-check-out');
  for (const input of [checkIn, checkOut]) {
    await expectTouchTarget(input);
    await input.scrollIntoViewIfNeeded();
    await expectContainedInViewport(page, input);
  }

  await checkIn.click();

  const popper = page.locator('.product-datepicker-popper').last();
  const calendar = popper.locator('.react-datepicker');
  await expect(calendar).toBeVisible();

  const header = page.locator('.site-header');
  const [headerBox, calendarBox] = await Promise.all([
    header.boundingBox(),
    calendar.boundingBox(),
  ]);
  expect(headerBox).not.toBeNull();
  expect(calendarBox).not.toBeNull();
  expect(calendarBox.y).toBeGreaterThanOrEqual(headerBox.y + headerBox.height - 1);
  await expectContainedInViewport(page, calendar);

  const navigationControls = popper.locator('.react-datepicker__navigation');
  await expect(navigationControls).not.toHaveCount(0);
  for (const control of await navigationControls.all()) {
    await expect(control).toBeVisible();
    await expectTouchTarget(control);
    await control.scrollIntoViewIfNeeded();
    await expectContainedInViewport(page, control);
  }

  const dateControls = popper.locator('.react-datepicker__day');
  await expect(dateControls).not.toHaveCount(0);
  for (const control of await dateControls.all()) {
    await expect(control).toBeVisible();
    await expectTouchTarget(control);
    await control.scrollIntoViewIfNeeded();
    await expectContainedInViewport(page, control);
  }

  await expectNoHorizontalOverflow(page);
}

test.describe('Mobile ProductDetail at the project viewport', () => {
  test('opens an in-viewport datepicker below the sticky header with reachable controls', async ({ page }) => {
    await exerciseProductDetail(page);
  });
});

test.describe('Mobile ProductDetail at 320x844', () => {
  test.use({ viewport: { width: 320, height: 844 } });

  test('opens an in-viewport datepicker below the sticky header with reachable controls', async ({ page }) => {
    await exerciseProductDetail(page);
  });
});
