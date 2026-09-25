// @ts-check
const { test, expect } = require('../fixtures/fixtures');

const CITY_SUGGESTIONS = ['Buenos Aires', 'Burzaco', 'Bariloche'];

/** @param {import('@playwright/test').Page} page */
async function mockHomeApi(
  page,
  searchRequests = [],
  { failInitialCategory = false, failInitialSearch = false } = {},
) {
  const retryState = {
    categoriesFailing: failInitialCategory,
    searchFailing: failInitialSearch,
  };

  await page.route('https://img.icons8.com/**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'image/svg+xml',
      body: '<svg xmlns="http://www.w3.org/2000/svg" width="1" height="1"/>',
    });
  });
  await page.route('**/api/auth/me', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: 'null',
    });
  });
  await page.route('**/api/auth/csrf', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: '{}',
    });
  });
  await page.route('**/api/categories', async (route) => {
    if (retryState.categoriesFailing) {
      await route.fulfill({ status: 500, contentType: 'application/json', body: '{}' });
      return;
    }

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: '[]',
    });
  });
  await page.route('**/api/lodgings/recommendations**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        lodgings: [],
        currentPage: 0,
        totalPages: 1,
        revision: 'mobile-home-test',
      }),
    });
  });
  await page.route('**/api/lodgings/cities**', async (route) => {
    const query = new URL(route.request().url()).searchParams.get('q')?.toLowerCase() ?? '';
    const suggestions = CITY_SUGGESTIONS.filter((city) => city.toLowerCase().startsWith(query));
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(suggestions),
    });
  });
  await page.route('**/api/lodgings/search**', async (route) => {
    searchRequests.push(route.request().url());
    if (retryState.searchFailing) {
      await route.fulfill({ status: 500, contentType: 'application/json', body: '{}' });
      return;
    }

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ lodgings: [], totalItems: 0, catalogItems: 0 }),
    });
  });

  return {
    allowCategoryRetry() {
      retryState.categoriesFailing = false;
    },
    allowSearchRetry() {
      retryState.searchFailing = false;
    },
  };
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

/** @param {import('@playwright/test').Locator} locator */
async function expectTouchTarget(locator) {
  const box = await locator.boundingBox();
  expect(box).not.toBeNull();
  expect(box.width).toBeGreaterThanOrEqual(44);
  expect(box.height).toBeGreaterThanOrEqual(44);
}

/**
 * @param {import('@playwright/test').Page} page
 * @param {string} placeholder
 */
async function inspectDatepicker(page, placeholder) {
  const input = page.getByPlaceholder(placeholder);
  await input.scrollIntoViewIfNeeded();
  await expect(input).toBeVisible();
  await expectTouchTarget(input);
  await input.click();
  await expect(input).toBeFocused();

  const popper = page.locator('.home-datepicker-popper').last();
  const calendar = popper.locator('.react-datepicker');
  await expect(calendar).toBeVisible();
  await calendar.scrollIntoViewIfNeeded();
  await expectContainedInViewport(page, calendar);
  await expectNoHorizontalOverflow(page);

  const navigationControls = popper.locator('.react-datepicker__navigation');
  await expect(navigationControls).not.toHaveCount(0);
  for (const control of await navigationControls.all()) {
    await expect(control).toBeVisible();
    await expectTouchTarget(control);
    await control.scrollIntoViewIfNeeded();
    await expectContainedInViewport(page, control);
  }

  const nextMonth = popper.locator('.react-datepicker__navigation--next');
  await nextMonth.focus();
  await expect(nextMonth).toBeFocused();
  await expectContainedInViewport(page, nextMonth);
  const currentMonth = popper.locator('.react-datepicker__current-month');
  const initialMonth = await currentMonth.innerText();
  await page.keyboard.press('Enter');
  await expect(currentMonth).not.toHaveText(initialMonth);

  const enabledInMonthDay = popper.locator(
    '.react-datepicker__day:not(.react-datepicker__day--outside-month):not(.react-datepicker__day--disabled)',
  ).first();
  await expect(enabledInMonthDay).toBeVisible();
  await expectTouchTarget(enabledInMonthDay);
  await expectContainedInViewport(page, enabledInMonthDay);

  await expectNoHorizontalOverflow(page);
  await page.keyboard.press('Escape');
  await expect(calendar).toHaveCount(0);
}

/**
 * @param {import('@playwright/test').Page} page
 * @param {import('../pages/HomePage')} homePage
 */
async function exerciseCityAutocomplete(page, homePage) {
  await mockHomeApi(page);
  await homePage.open('/');

  const input = page.getByRole('combobox');
  await input.fill('Bu');
  const listbox = page.getByRole('listbox', { name: 'Sugerencias de ciudades' });
  await expect(listbox).toBeVisible();

  const buenosAires = page.getByRole('option', { name: 'Buenos Aires' });
  const burzaco = page.getByRole('option', { name: 'Burzaco' });
  await expect(buenosAires).toBeVisible();
  await expect(burzaco).toBeVisible();
  const buenosAiresId = await buenosAires.getAttribute('id');
  const burzacoId = await burzaco.getAttribute('id');
  expect(buenosAiresId).not.toBeNull();
  expect(burzacoId).not.toBeNull();

  await input.press('ArrowDown');
  await expect(input).toHaveAttribute('aria-activedescendant', buenosAiresId);
  await input.press('ArrowDown');
  await expect(input).toHaveAttribute('aria-activedescendant', burzacoId);
  await input.press('Enter');
  await expect(input).toHaveValue('Burzaco');
  await expect(input).toHaveAttribute('aria-expanded', 'false');

  await input.fill('Ba');
  const bariloche = page.getByRole('option', { name: 'Bariloche' });
  await expect(bariloche).toBeVisible();
  await bariloche.tap();
  await expect(input).toHaveValue('Bariloche');
  await expect(input).toHaveAttribute('aria-expanded', 'false');
}

/**
 * @param {import('@playwright/test').Page} page
 * @param {import('../pages/HomePage')} homePage
 */
async function exerciseSearchSubmission(page, homePage) {
  const searchRequests = [];
  await mockHomeApi(page, searchRequests);
  await homePage.open('/');

  await page.getByPlaceholder('Ciudad').fill('Buenos Aires');
  await page.getByRole('button', { name: 'Buscar' }).click();

  await expect(page).toHaveURL(/\/\?city=Buenos(?:%20|\+)Aires$/);
  await expect(page.getByRole('heading', { name: 'Resultados de búsqueda' })).toBeVisible();
  const searchUrl = new URL(searchRequests.at(-1));
  expect(searchUrl.searchParams.get('city')).toBe('Buenos Aires');
  await expectNoHorizontalOverflow(page);
}

/**
 * @param {import('@playwright/test').Page} page
 * @param {import('../pages/HomePage')} homePage
 */
async function exerciseRetryControls(page, homePage) {
  const homeApi = await mockHomeApi(page, [], {
    failInitialCategory: true,
    failInitialSearch: true,
  });
  await homePage.open('/?city=Buenos%20Aires');

  const categoryAlert = page.getByRole('alert').filter({ hasText: 'categorías' });
  const searchAlert = page.getByRole('alert').filter({ hasText: 'búsqueda' });
  const categoryRetry = categoryAlert.getByRole('button', { name: 'Reintentar' });
  const searchRetry = searchAlert.getByRole('button', { name: 'Reintentar' });

  await expect(categoryAlert).toBeVisible();
  await expect(searchAlert).toBeVisible();
  for (const retryControl of [categoryRetry, searchRetry]) {
    await retryControl.scrollIntoViewIfNeeded();
    await expectTouchTarget(retryControl);
    await expectContainedInViewport(page, retryControl);
    await retryControl.focus();
    await expect(retryControl).toBeFocused();
    await expect(retryControl).toHaveCSS('outline-style', 'solid');
  }
  await expectNoHorizontalOverflow(page);

  homeApi.allowCategoryRetry();
  await categoryRetry.click();
  await expect(categoryAlert).toHaveCount(0);
  await expect(page.getByText('No hay categorías disponibles.')).toBeVisible();
  await expect(searchAlert).toBeVisible();

  homeApi.allowSearchRetry();
  await searchRetry.click();
  await expect(searchAlert).toHaveCount(0);
  await expect(page.getByRole('heading', { name: 'Resultados de búsqueda' })).toBeVisible();
  await expect(page.getByText('0 resultados de 0 alojamientos')).toBeVisible();
  await expectNoHorizontalOverflow(page);
}

test.use({ hasTouch: true });

test.describe('Mobile Home city search', () => {
  test('supports keyboard navigation and touch selection for city suggestions', async ({ page, homePage }) => {
    await exerciseCityAutocomplete(page, homePage);
  });
});

test.describe('Mobile Home datepickers', () => {
  test('keeps check-in and check-out calendars reachable and contained', async ({ page, homePage }) => {
    await mockHomeApi(page);
    await homePage.open('/');

    await inspectDatepicker(page, 'Check-in');
    await inspectDatepicker(page, 'Check-out');
  });
});

test.describe('Mobile Home search submission', () => {
  test('submits the city query on Home and renders deterministic results', async ({ page, homePage }) => {
    await exerciseSearchSubmission(page, homePage);
  });
});

test.describe('Home retry controls on desktop', () => {
  test.use({ viewport: { width: 1280, height: 844 } });

  test('keeps failed category and search retries accessible through successful empty states', async ({ page, homePage }) => {
    await exerciseRetryControls(page, homePage);
  });
});

test.describe('Home retry controls at 390x844', () => {
  test.use({ viewport: { width: 390, height: 844 } });

  test('keeps failed category and search retries accessible through successful empty states', async ({ page, homePage }) => {
    await exerciseRetryControls(page, homePage);
  });
});

test.describe('Mobile Home at 320x844', () => {
  test.use({ viewport: { width: 320, height: 844 } });

  test('supports city autocomplete through keyboard and touch without overflow', async ({ page, homePage }) => {
    await exerciseCityAutocomplete(page, homePage);
    await expectNoHorizontalOverflow(page);
  });

  test('submits the city query on Home and renders results without overflow', async ({ page, homePage }) => {
    await exerciseSearchSubmission(page, homePage);
  });

  test('keeps the search surface and datepicker controls reachable without overflow', async ({ page, homePage }) => {
    await mockHomeApi(page);
    await homePage.open('/');

    await expectNoHorizontalOverflow(page);
    await inspectDatepicker(page, 'Check-in');
    await expectNoHorizontalOverflow(page);
  });

  test('keeps failed category and search retries accessible through successful empty states', async ({ page, homePage }) => {
    await exerciseRetryControls(page, homePage);
  });
});
