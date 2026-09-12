// @ts-check
const { test, expect } = require('../fixtures/fixtures');

const LODGING_ID = 77;
const CSRF_TOKEN = 'mobile-reviews-csrf-token';
const LONG_TEXT = 'Reseñista'.repeat(32);
const LONG_COMMENT = 'experiencia'.repeat(80);

const lodging = {
  id: LODGING_ID,
  name: 'Alojamiento mobile de prueba',
  city: 'Mendoza',
  country: 'Argentina',
  description: 'Detalle controlado para probar reseñas.',
  pricePerNight: 100,
  imageUrls: [],
  features: [],
};

const ratings = {
  average: 4,
  count: 1,
  ratings: [{
    id: 1,
    userName: LONG_TEXT,
    score: 4,
    comment: LONG_COMMENT,
    createdAt: '2026-01-15T12:00:00Z',
  }],
};

/** @param {import('@playwright/test').Locator} locator */
async function expectTouchTarget(locator) {
  const box = await locator.boundingBox();
  expect(box).not.toBeNull();
  expect(box.width).toBeGreaterThanOrEqual(44);
  expect(box.height).toBeGreaterThanOrEqual(44);
}

/** @param {import('@playwright/test').Page} page */
async function expectNoReviewsOverflow(page) {
  const widths = await page.evaluate(() => {
    const selectors = ['html', '.ratings-section', '.review-item', '.review-comment'];
    return selectors.map((selector) => {
      const element = document.querySelector(selector);
      if (!element) throw new Error(`Missing ${selector}`);
      return { selector, clientWidth: element.clientWidth, scrollWidth: element.scrollWidth };
    });
  });
  for (const width of widths) {
    expect(width.scrollWidth, width.selector).toBeLessThanOrEqual(width.clientWidth);
  }
}

/** @param {import('@playwright/test').Page} page */
async function interceptReviewsApi(page) {
  await page.context().addCookies([{
    name: 'XSRF-TOKEN',
    value: CSRF_TOKEN,
    url: 'http://localhost:5173',
    sameSite: 'Lax',
  }]);

  await page.route('**/api/auth/me', (route) => route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify({ id: 9, firstName: 'Mobile', lastName: 'Reviewer', email: 'mobile@example.test' }),
  }));
  await page.route('**/api/auth/csrf', (route) => route.fulfill({ status: 204 }));
  await page.route(`**/api/lodgings/${LODGING_ID}`, (route) => route.fulfill({
    status: 200, contentType: 'application/json', body: JSON.stringify(lodging),
  }));
  await page.route(`**/api/lodgings/${LODGING_ID}/availability**`, (route) => route.fulfill({
    status: 200, contentType: 'application/json', body: JSON.stringify({ occupiedRanges: [] }),
  }));
  await page.route(`**/api/ratings/lodging/${LODGING_ID}/eligibility`, (route) => route.fulfill({
    status: 200, contentType: 'application/json', body: JSON.stringify({ eligible: true, reason: 'ELIGIBLE' }),
  }));
  await page.route('**/api/ratings', async (route, request) => {
    if (request.method() !== 'POST') return route.fallback();
    expect(request.headers()['x-xsrf-token']).toBe(CSRF_TOKEN);
    await route.fulfill({
      status: 400,
      contentType: 'application/json',
      body: JSON.stringify({ error: 'No se pudo guardar la reseña.' }),
    });
  });
}

/** @param {import('@playwright/test').Page} page */
async function exerciseReviews(page) {
  await interceptReviewsApi(page);
  const ratingsRoute = `**/api/ratings/lodging/${LODGING_ID}`;
  await page.route(ratingsRoute, (route) => route.fulfill({
    status: 503, contentType: 'application/json', body: JSON.stringify({ error: 'Temporal' }),
  }));
  await page.goto(`/lodgings/${LODGING_ID}`);

  const retry = page.locator('.ratings-alert').getByRole('button', { name: 'Reintentar' });
  await expect(retry).toBeVisible();
  await expectTouchTarget(retry);
  await page.unroute(ratingsRoute);
  await page.route(ratingsRoute, (route) => route.fulfill({
    status: 200, contentType: 'application/json', body: JSON.stringify(ratings),
  }));
  await retry.click();

  const reviewScore = page.getByRole('img', { name: 'Puntaje 4 de 5 estrellas' });
  await expect(reviewScore).toBeVisible();
  await expect(reviewScore.locator('[aria-hidden="true"]')).toHaveCount(5);
  await expectNoReviewsOverflow(page);

  const scoreControls = page.locator('.star-selector button');
  await expect(scoreControls).toHaveCount(5);
  for (const control of await scoreControls.all()) await expectTouchTarget(control);

  const firstScore = page.getByRole('button', { name: '1 estrella' });
  await firstScore.focus();
  await page.keyboard.press('Tab');
  const secondScore = page.getByRole('button', { name: '2 estrellas' });
  await expect(secondScore).toBeFocused();
  await page.keyboard.press('Space');
  await expect(secondScore).toHaveAttribute('aria-pressed', 'true');

  const comment = page.getByLabel('Comentario');
  await comment.fill('Comentario que debe conservarse');
  const submit = page.getByRole('button', { name: 'Enviar reseña' });
  await expectTouchTarget(submit);
  await submit.focus();
  await submit.click();

  await expect(page.getByRole('alert')).toContainText('No se pudo guardar la reseña.');
  await expect(secondScore).toHaveAttribute('aria-pressed', 'true');
  await expect(comment).toHaveValue('Comentario que debe conservarse');
  await expect(submit).toBeEnabled();
  await expect(submit).toBeFocused();
  await page.keyboard.press('Shift+Tab');
  await expect(comment).toBeFocused();
  await page.keyboard.press('Tab');
  await expect(submit).toBeFocused();
  await expectNoReviewsOverflow(page);
}

test.describe('Mobile ReviewsSection at the project viewport', () => {
  test('keeps reviews accessible, usable, and contained at 390x844', async ({ page }) => {
    await exerciseReviews(page);
  });
});

test.describe('Mobile ReviewsSection at 320x844', () => {
  test.use({ viewport: { width: 320, height: 844 } });

  test('keeps reviews accessible, usable, and contained at 320x844', async ({ page }) => {
    await exerciseReviews(page);
  });
});
