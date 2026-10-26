// @ts-check
const { test, expect } = require('../fixtures/fixtures');

/** @param {import('@playwright/test').Locator} locator */
async function boxOf(locator) {
  const box = await locator.boundingBox();
  if (box === null) throw new Error('Calendar control has no measurable bounding box');
  return box;
}

/**
 * @param {import('@playwright/test').Page} page
 * @param {import('@playwright/test').Locator} locator
 */
async function expectContained(page, locator) {
  const box = await boxOf(locator);
  const viewport = page.viewportSize();
  if (viewport === null) throw new Error('Calendar test requires a fixed viewport');
  expect(box.x).toBeGreaterThanOrEqual(-1);
  expect(box.y).toBeGreaterThanOrEqual(-1);
  expect(box.x + box.width).toBeLessThanOrEqual(viewport.width + 1);
  expect(box.y + box.height).toBeLessThanOrEqual(viewport.height + 1);
}

/**
 * Every API response is synthetic; external traffic and writes are blocked.
 * @param {import('@playwright/test').Page} page
 * @param {string} baseURL
 */
async function mockHome(page, baseURL) {
  /** @type {string[]} */
  const writes = [];
  /** @type {string[]} */
  const unexpectedApis = [];
  /** @type {URL[]} */
  const searches = [];
  /** @type {string[]} */
  const blockedResources = [];
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
    if (!url.pathname.startsWith('/api/')) {
      if (url.origin !== origin) {
        blockedResources.push(url.href);
        await route.abort('blockedbyclient');
        return;
      }
      await route.continue();
      return;
    }
    // CI serves the API on a different port; keep every API response mocked.
    let data;
    if (url.pathname === '/api/auth/me') {
      data = null;
    } else if (url.pathname === '/api/auth/csrf') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        headers: { 'Set-Cookie': 'XSRF-TOKEN=home-calendar-fixture; Path=/; SameSite=Lax' },
        body: '{}',
      });
      return;
    } else if (url.pathname === '/api/categories') {
      data = [];
    } else if (url.pathname === '/api/lodgings/cities') {
      data = ['Bariloche'];
    } else if (url.pathname === '/api/lodgings/recommendations') {
      data = { lodgings: [], currentPage: 0, totalPages: 1, revision: 'calendar-fixture' };
    } else if (url.pathname === '/api/lodgings/search') {
      searches.push(url);
      const currentPage = Number(url.searchParams.get('page') || 0);
      data = {
        lodgings: [{ id: 31 + currentPage, name: `Calendar result ${currentPage + 1}`, city: 'Bariloche', country: 'Argentina', pricePerNight: 100, imageUrls: [], features: [] }],
        currentPage, totalPages: 2, totalItems: 2, catalogItems: 2,
      };
    } else if (url.pathname.startsWith('/api/ratings/')) {
      data = { average: 0, count: 0, ratings: [] };
    } else {
      unexpectedApis.push(url.pathname);
      await route.abort('blockedbyclient');
      return;
    }
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(data) });
  });
  return { writes, unexpectedApis, searches, blockedResources };
}

/**
 * @param {import('@playwright/test').Page} page
 * @param {boolean} stacked
 */
async function inspectCalendar(page, stacked) {
  const popper = page.locator('.home-datepicker-popper');
  await expect(popper).toBeVisible();
  const months = popper.locator('.react-datepicker__month-container:visible');
  await expect(months).toHaveCount(2);
  const first = await boxOf(months.nth(0));
  const second = await boxOf(months.nth(1));
  if (stacked) {
    expect(second.y).toBeGreaterThanOrEqual(first.y + first.height - 1);
    expect(Math.abs(first.x - second.x)).toBeLessThanOrEqual(1);
    const header = await boxOf(page.locator('.site-header'));
    expect((await boxOf(popper)).y).toBeGreaterThanOrEqual(header.y + header.height - 1);
    await expectContained(page, popper);
  } else {
    expect(Math.abs(first.y - second.y)).toBeLessThanOrEqual(1);
    expect(second.x).toBeGreaterThanOrEqual(first.x + first.width - 1);
    await expectContained(page, popper);
  }
  const controls = popper.locator('.react-datepicker__navigation, .react-datepicker__day:visible');
  for (const control of await controls.all()) {
    const box = await boxOf(control);
    expect(box.width).toBeGreaterThanOrEqual(stacked ? 44 : 36);
    expect(box.height).toBeGreaterThanOrEqual(stacked ? 44 : 36);
    await control.scrollIntoViewIfNeeded();
    await expectContained(page, control);
  }
  const titles = popper.locator('.react-datepicker__current-month');
  const initialTitles = await titles.allTextContents();
  const next = popper.locator('.react-datepicker__navigation--next');
  await next.scrollIntoViewIfNeeded();
  await next.focus();
  await expect(next).toBeFocused();
  await next.press('Enter');
  await expect(titles.first()).not.toHaveText(initialTitles[0]);
  await expect(titles.nth(1)).not.toHaveText(initialTitles[1]);
  const previous = popper.locator('.react-datepicker__navigation--previous');
  await previous.scrollIntoViewIfNeeded();
  await previous.click();
  await expect(titles.first()).toHaveText(initialTitles[0]);
  await expect(titles.nth(1)).toHaveText(initialTitles[1]);
  return popper;
}

test('keeps writes, unknown APIs and external non-API resources blocked', async ({ page, baseURL }) => {
  if (baseURL === undefined) throw new Error('Calendar test requires baseURL');
  const evidence = await mockHome(page, baseURL);
  await page.goto('/');
  await expect(page.getByRole('combobox', { name: 'Ciudad' })).toBeVisible();

  // These loopback URLs have no backend. Every probe must be intercepted.
  const apiOrigin = new URL(baseURL);
  apiOrigin.port = '5199';
  const probes = [
    { url: new URL('/api/lodgings/search', apiOrigin).href, method: 'POST' },
    { url: new URL('/api/mock-guard-missing', apiOrigin).href, method: 'GET' },
    { url: new URL('/mock-external-resource', apiOrigin).href, method: 'GET' },
  ];
  for (const probe of probes) {
    const result = await page.evaluate(async ({ url, method }) => {
      try {
        await fetch(url, { method });
        return { rejected: false, name: '' };
      } catch (error) {
        return { rejected: true, name: error instanceof Error ? error.name : String(error) };
      }
    }, probe);
    expect(result).toEqual({ rejected: true, name: 'TypeError' });
  }
  expect(evidence.writes).toEqual(['POST /api/lodgings/search']);
  expect(evidence.unexpectedApis).toEqual(['/api/mock-guard-missing']);
  expect(evidence.searches).toEqual([]);
  const externalURL = probes[2].url;
  expect(evidence.blockedResources.filter(url => url === externalURL)).toEqual([externalURL]);
});

for (const width of [320, 390, 768, 769, 1440]) {
  test.describe(`Home double calendar at ${width}px`, () => {
    test.use({ viewport: { width, height: width <= 390 ? 844 : 900 } });
    test('selects dates in real two-month pickers and preserves the search on page changes', async ({ page, baseURL }) => {
      if (baseURL === undefined) throw new Error('Calendar test requires baseURL');
      const evidence = await mockHome(page, baseURL);
      await page.goto('/');
      await page.getByRole('combobox', { name: 'Ciudad' }).fill('Bariloche');
      const checkIn = page.getByRole('textbox', { name: 'Check-in', exact: true });
      const checkOut = page.getByRole('textbox', { name: 'Check-out', exact: true });
      await checkIn.click();
      let popper = await inspectCalendar(page, width <= 768);
      const januaryIn = popper.locator('.react-datepicker__month-container').filter({
        has: page.locator('.react-datepicker__current-month', { hasText: /^enero 2027$/i }),
      });
      await januaryIn.locator('.react-datepicker__day--010:not(.react-datepicker__day--outside-month)').click();
      await expect(checkIn).toHaveValue('10/01/2027');
      await expect(popper).not.toBeVisible();
      await checkOut.click();
      popper = await inspectCalendar(page, width <= 768);
      const januaryOut = popper.locator('.react-datepicker__month-container').filter({
        has: page.locator('.react-datepicker__current-month', { hasText: /^enero 2027$/i }),
      });
      await januaryOut.locator('.react-datepicker__day--013:not(.react-datepicker__day--outside-month)').click();
      await expect(checkOut).toHaveValue('13/01/2027');
      await expect(checkIn).toHaveValue('10/01/2027');
      await expect(popper).not.toBeVisible();
      await checkIn.focus();
      await checkIn.press('ArrowDown');
      await expect(popper).toBeVisible();
      await expect(popper.locator('.react-datepicker__day:focus')).toHaveCount(1);
      await page.keyboard.press('Escape');
      await expect(popper).not.toBeVisible();
      // Stable focus restoration also fails on the baseline; tracked separately in DESIGN.md.
      await page.getByRole('button', { name: 'Buscar', exact: true }).click();
      await expect(page.locator('.search-results')).toContainText('Calendar result 1');
      const pagination = page.getByRole('navigation', { name: 'Paginación de resultados de búsqueda' });
      await pagination.getByRole('button', { name: 'Siguiente' }).click();
      await expect(page.locator('.search-results')).toContainText('Calendar result 2');
      const query = new URL(page.url()).searchParams;
      expect(query.get('city')).toBe('Bariloche');
      expect(query.get('checkIn')).toBe('2027-01-10');
      expect(query.get('checkOut')).toBe('2027-01-13');
      expect(query.get('page')).toBe('1');
      expect(evidence.searches.map(url => url.searchParams.get('page') || '0')).toEqual(['0', '1']);
      const size = await page.evaluate(() => ({ client: document.documentElement.clientWidth, scroll: document.documentElement.scrollWidth }));
      expect(size.scroll).toBeLessThanOrEqual(size.client);
      expect(evidence.unexpectedApis).toEqual([]);
      expect(evidence.writes).toEqual([]);
    });
  });
}
