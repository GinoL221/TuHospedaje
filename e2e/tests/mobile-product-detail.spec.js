// @ts-check
const { test, expect } = require('../fixtures/fixtures');

const LODGING_ID = 6;

/** @param {import('@playwright/test').Locator} locator */
async function expectTouchTarget(locator) {
  const box = await locator.boundingBox();
  if (box === null) throw new Error('Touch target has no measurable bounding box');
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
  if (box === null) throw new Error('Control has no measurable bounding box');
  expect(box).not.toBeNull();
  const viewport = page.viewportSize();
  if (viewport === null) throw new Error('Calendar test requires a fixed viewport');
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

/**
 * All API responses are fixtures. No real auth, availability, or writes.
 * @param {import('@playwright/test').Page} page
 * @param {string} baseURL
 */
async function mockProductDetail(page, baseURL) {
  /** @type {string[]} */
  const writes = [];
  const origin = new URL(baseURL).origin;
  await page.clock.setFixedTime(new Date('2026-12-01T12:00:00Z'));
  await page.route('**/*', async route => {
    const request = route.request();
    const url = new URL(request.url());
    if (!['GET', 'HEAD'].includes(request.method())) {
      writes.push(request.method() + ' ' + url.pathname);
      await route.abort('blockedbyclient');
      return;
    }
    if (url.pathname.startsWith('/api/')) {
      let status = 200;
      let data = null;
      if (url.pathname === '/api/auth/me') {
        status = 401;
        data = { error: 'Anonymous test session' };
      } else if (url.pathname === `/api/lodgings/${LODGING_ID}`) {
        data = {
          id: LODGING_ID,
          name: 'Calendar test lodging',
          city: 'Bariloche',
          country: 'Argentina',
          description: 'Synthetic calendar fixture.',
          pricePerNight: 100,
          imageUrls: [],
          features: [],
          policies: [],
        };
      } else if (url.pathname === `/api/lodgings/${LODGING_ID}/availability`) {
        data = { available: true, occupiedRanges: [{ checkIn: '2026-12-20', checkOut: '2026-12-22' }] };
      } else if (url.pathname.startsWith('/api/ratings/')) {
        data = { average: 0, count: 0, ratings: [] };
      } else {
        throw new Error('Unexpected fixture API: ' + url.pathname);
      }
      await route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(data) });
      return;
    }
    if (url.origin !== origin) {
      await route.abort('blockedbyclient');
      return;
    }
    await route.continue();
  });
  return writes;
}

/**
 * @param {import('@playwright/test').Page} page
 * @param {string} baseURL
 * @param {boolean} stacked
 */
async function exerciseProductDetail(page, baseURL, stacked) {
  const writes = await mockProductDetail(page, baseURL);
  await page.goto(`/lodgings/${LODGING_ID}`);
  await expect(page.locator('.product-detail h1')).toHaveText('Calendar test lodging');
  const checkIn = page.locator('#product-check-in');
  const checkOut = page.locator('#product-check-out');
  await expect(checkIn).toBeEnabled();
  await expectNoHorizontalOverflow(page);

  for (const input of [checkIn, checkOut]) {
    await expectTouchTarget(input);
    await input.scrollIntoViewIfNeeded();
    await expectContainedInViewport(page, input);
  }
  await checkIn.click();
  const popper = page.locator('.product-datepicker-popper');
  const calendar = popper.locator('.react-datepicker');
  await expect(calendar).toBeVisible();
  const months = popper.locator('.react-datepicker__month-container:visible');
  await expect(months).toHaveCount(2);
  const [first, second] = await Promise.all([months.nth(0).boundingBox(), months.nth(1).boundingBox()]);
  if (first === null || second === null) throw new Error('Both calendar months must have measurable bounding boxes');
  expect(first).not.toBeNull();
  expect(second).not.toBeNull();
  if (stacked) {
    expect(second.y).toBeGreaterThanOrEqual(first.y + first.height - 1);
    expect(Math.abs(first.x - second.x)).toBeLessThanOrEqual(1);
    const headerBox = await page.locator('.site-header').boundingBox();
    const popperBox = await popper.boundingBox();
    if (headerBox === null || popperBox === null) throw new Error('Header and calendar popup must have measurable bounding boxes');
    expect(popperBox.y).toBeGreaterThanOrEqual(headerBox.y + headerBox.height - 1);
    // The bounded popup scrolls internally when two months exceed viewport height.
    await expectContainedInViewport(page, popper);
  } else {
    expect(Math.abs(first.y - second.y)).toBeLessThanOrEqual(1);
    expect(second.x).toBeGreaterThanOrEqual(first.x + first.width - 1);
  }

  const next = popper.locator('.react-datepicker__navigation--next');
  const previous = popper.locator('.react-datepicker__navigation--previous');
  // At minDate's month there is no earlier selectable month to navigate to.
  await expect(previous).toHaveCount(0);
  await expect(next).toBeVisible();
  await expectTouchTarget(next);
  await next.scrollIntoViewIfNeeded();
  await expectContainedInViewport(page, next);
  const titles = calendar.locator('.react-datepicker__current-month');
  const initialTitles = await titles.allTextContents();
  await next.click();
  await expect(titles.first()).not.toHaveText(initialTitles[0]);
  await expect(titles.nth(1)).not.toHaveText(initialTitles[1]);
  await expect(previous).toBeVisible();
  await expectTouchTarget(previous);
  await previous.scrollIntoViewIfNeeded();
  await expectContainedInViewport(page, previous);
  await previous.click();
  await expect(titles.first()).toHaveText(initialTitles[0]);
  await expect(titles.nth(1)).toHaveText(initialTitles[1]);
  await expect(previous).toHaveCount(0);

  const occupied = months.first().locator('.react-datepicker__day--020:not(.react-datepicker__day--outside-month)');
  await expect(occupied).toHaveAttribute('aria-disabled', 'true');
  const days = popper.locator('.react-datepicker__day:visible');
  for (const control of await days.all()) {
    await expectTouchTarget(control);
    await control.scrollIntoViewIfNeeded();
    await expectContainedInViewport(page, control);
  }

  // Select a stay using the newly visible second month, then preserve normal close/reopen behavior.
  await months.nth(1).locator('.react-datepicker__day--010:not(.react-datepicker__day--outside-month)').click();
  await expect(checkIn).toHaveValue('10/01/2027');
  const january = calendar.locator('.react-datepicker__month-container').filter({
    has: page.locator('.react-datepicker__current-month', { hasText: /^January 2027$/ }),
  });
  await january.locator('.react-datepicker__day--013:not(.react-datepicker__day--outside-month)').click();
  await expect(checkOut).toHaveValue('13/01/2027');
  await expect(popper).not.toBeVisible();
  await expect(page.locator('.total-estimate')).toContainText('$300');
  await expect(page.locator('.total-estimate')).toContainText('3 noches');
  await checkIn.focus();
  await checkIn.press('ArrowDown');
  await expect(popper).toBeVisible();
  await checkIn.press('Escape');
  await expect(popper).not.toBeVisible();
  await expect(checkIn).toBeFocused();
  await expectNoHorizontalOverflow(page);
  expect(writes).toEqual([]);
}

for (const width of [320, 390, 768, 769, 1440]) {
  const stacked = width <= 768;
  test.describe(`ProductDetail double calendar at ${width}px`, () => {
    test.use({ viewport: { width, height: width <= 390 ? 844 : 900 } });
    test(`shows two ${stacked ? 'stacked' : 'side-by-side'} months with reachable date controls`, async ({ page, baseURL }) => {
      if (baseURL === undefined) throw new Error('Calendar test requires baseURL');
      await exerciseProductDetail(page, baseURL, stacked);
    });
  });
}
