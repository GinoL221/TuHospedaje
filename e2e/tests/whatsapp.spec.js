// @ts-check
const { test, expect } = require('../fixtures/fixtures');

/**
 * WU10 — WhatsApp handoff feedback (US-34.1, US-34.2, US-34.3).
 * The declarative link opens WhatsApp in a separate browsing context; these
 * assertions verify its client-side contract and do not claim message delivery.
 * Invalid/missing configuration (US-34.2-S2) cannot be exercised here because
 * `VITE_WHATSAPP_NUMBER` is inlined at Vite build time, not readable at runtime;
 * that scenario is covered by
 * `frontend/src/components/WhatsAppButton/WhatsAppButton.test.jsx` (Vitest,
 * `vi.stubEnv`) — see the traceability matrix.
 */

/** @param {import('@playwright/test').Page} page */
function whatsappLink(page) {
  return page.getByRole('link', { name: 'Contactar por WhatsApp' });
}

test.describe('WhatsApp handoff', () => {
  test('the declarative link is visible and has the secure WhatsApp destination for an anonymous visitor', async ({ page, homePage }) => {
    await homePage.open('/');

    const link = whatsappLink(page);
    await expect(link).toBeVisible();
    await expect(link).toHaveAttribute('target', '_blank');
    await expect(link).toHaveAttribute('rel', expect.stringContaining('noopener noreferrer'));
    await expect(link).toHaveAttribute('href', expect.stringMatching(/^https:\/\/wa\.me\//));
  });

  test.describe('authenticated', () => {
    test.skip(!process.env.TEST_USER_EMAIL, 'Set TEST_USER_EMAIL and TEST_USER_PASSWORD in .env');

    test('the link remains available with equivalent behavior when logged in', async ({ page, authUser }) => {
      await page.goto('/');
      await expect(whatsappLink(page)).toBeVisible();
    });
  });
});
