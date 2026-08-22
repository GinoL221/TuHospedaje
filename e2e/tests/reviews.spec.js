// @ts-check
const { test, expect } = require('../fixtures/fixtures');

/**
 * WU8/WU9 — Rating eligibility gating and submission feedback (US-28.1,
 * US-28.2). LODGING_ELIGIBLE has a CONFIRMED reservation with a past
 * checkout for the TEST_USER fixture (seeded directly into the isolated
 * dev database — no booking API creates a past-dated stay). LODGING_OTHER
 * has no reservation for that user, proving per-lodging eligibility scope
 * (US-28.1-S6) doubles as the "ineligible" scenario.
 */
const LODGING_ELIGIBLE = 1;
const LODGING_OTHER = 2;

test.describe('Reviews — eligibility gating', () => {
  test('anonymous visitors see public ratings but no eligibility fetch or form', async ({ page, homePage }) => {
    let eligibilityCalls = 0;
    page.on('request', (req) => {
      if (req.url().includes('/ratings/lodging/') && req.url().includes('/eligibility')) {
        eligibilityCalls += 1;
      }
    });

    await page.goto(`/lodgings/${LODGING_ELIGIBLE}`);
    await expect(page.locator('.ratings-section h2')).toHaveText('Reseñas');
    await expect(page.locator('.review-form')).toHaveCount(0);
    expect(eligibilityCalls).toBe(0);
  });

  test.describe('authenticated', () => {
    test.skip(!process.env.TEST_USER_EMAIL, 'Set TEST_USER_EMAIL and TEST_USER_PASSWORD in .env');

    test('an ineligible user sees an explanation and no submission form', async ({ page, authUser }) => {
      await page.goto(`/lodgings/${LODGING_OTHER}`);
      await expect(page.locator('.review-form h3')).toHaveText('Dejá tu reseña');
      await expect(
        page.getByText('Todavía no tenés una estadía confirmada y finalizada en este alojamiento'),
      ).toBeVisible();
      await expect(page.locator('.star-selector')).toHaveCount(0);
    });

    test('eligibility failure shows an accessible alert with retry', async ({ page, authUser }) => {
      const pattern = '**/ratings/lodging/*/eligibility';
      // Fail every request while routed — see home-recommendations.spec.js
      // for why a "fail-once" toggle races with StrictMode's double effect.
      await page.route(pattern, async (route) => {
        await route.fulfill({ status: 500, contentType: 'application/json', body: '{"error":"boom"}' });
      });

      await page.goto(`/lodgings/${LODGING_ELIGIBLE}`);
      const alert = page.locator('.eligibility-alert[role="alert"]');
      await expect(alert).toBeVisible();
      await page.unroute(pattern);
      await alert.getByRole('button', { name: 'Reintentar' }).click();
      await expect(page.locator('.star-selector')).toBeVisible();
    });

    test('an eligible user can select a score and submit; a server rejection keeps the score/comment and shows an inline alert', async ({ page, authUser }) => {
      await page.goto(`/lodgings/${LODGING_ELIGIBLE}`);
      await expect(page.locator('.star-selector')).toBeVisible();

      await page.getByRole('button', { name: '4 estrellas' }).click();
      await expect(page.getByRole('button', { name: '4 estrellas' })).toHaveAttribute('aria-pressed', 'true');
      await page.locator('.review-form textarea').fill('Excelente ubicación y atención.');

      await page.route('**/ratings', async (route, request) => {
        if (request.method() !== 'POST') return route.continue();
        await route.fulfill({ status: 400, contentType: 'application/json', body: '{"error":"No se pudo guardar la reseña."}' });
      });
      await page.getByRole('button', { name: 'Enviar reseña' }).click();

      const submitAlert = page.locator('.submit-error[role="alert"]');
      await expect(submitAlert).toHaveText('No se pudo guardar la reseña.');
      await expect(page.getByRole('button', { name: '4 estrellas' })).toHaveAttribute('aria-pressed', 'true');
      await expect(page.locator('.review-form textarea')).toHaveValue('Excelente ubicación y atención.');
    });

    test('a real successful submission refreshes the public ratings list', async ({ page, authUser }) => {
      await page.goto(`/lodgings/${LODGING_ELIGIBLE}`);
      await expect(page.locator('.star-selector')).toBeVisible();

      // Rating is an upsert per (user, lodging): the comment text is the
      // rerun-safe assertion, since a rerun updates rather than duplicates.
      const uniqueComment = `Volvería sin dudarlo (${Date.now()}).`;
      await page.getByRole('button', { name: '5 estrellas' }).click();
      await page.locator('.review-form textarea').fill(uniqueComment);
      await page.getByRole('button', { name: 'Enviar reseña' }).click();

      await expect(page.locator('.submit-error')).toHaveCount(0);
      await expect(page.locator('.review-form textarea')).toHaveValue('');
      await expect(page.locator('.reviews-list')).toContainText(uniqueComment);
    });
  });
});
