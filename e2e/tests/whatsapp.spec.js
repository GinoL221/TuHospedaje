// @ts-check
const { test, expect } = require('../fixtures/fixtures');

/**
 * WU10 — WhatsApp handoff feedback (US-34.1, US-34.2, US-34.3). `window.open`
 * is stubbed with a plain fake-window object in every test — no real
 * WhatsApp destination is ever contacted. Invalid/missing configuration
 * (US-34.2-S2) cannot be exercised here because `VITE_WHATSAPP_NUMBER` is
 * inlined at Vite build time, not readable at runtime; that scenario is
 * covered by `frontend/src/components/WhatsAppButton/WhatsAppButton.test.jsx`
 * (Vitest, `vi.stubEnv`) — see the traceability matrix.
 */

/** @param {import('@playwright/test').Page} page */
async function stubWindowOpen(page, { returnsNull = false, throwsOnAssign = false } = {}) {
  await page.evaluate(
    ({ returnsNull, throwsOnAssign }) => {
      // @ts-ignore
      window.__handoff = { opened: false, openerNulled: false, location: null, closed: false };
      window.open = () => {
        // @ts-ignore
        window.__handoff.opened = true;
        if (returnsNull) return null;
        return {
          set opener(v) {
            // @ts-ignore
            window.__handoff.openerNulled = v === null;
          },
          get opener() {
            return null;
          },
          set location(v) {
            if (throwsOnAssign) throw new Error('blocked by test double');
            // @ts-ignore
            window.__handoff.location = v;
          },
          close: () => {
            // @ts-ignore
            window.__handoff.closed = true;
          },
        };
      };
    },
    { returnsNull, throwsOnAssign },
  );
}

/** @param {import('@playwright/test').Page} page */
function whatsappButton(page) {
  return page.getByRole('button', { name: 'Contactar por WhatsApp' });
}

test.describe('WhatsApp handoff', () => {
  test('the control is visible and operable for an anonymous visitor', async ({ page, homePage }) => {
    await homePage.open('/');
    await expect(whatsappButton(page)).toBeVisible();
    await expect(whatsappButton(page)).toBeEnabled();
  });

  test.describe('authenticated', () => {
    test.skip(!process.env.TEST_USER_EMAIL, 'Set TEST_USER_EMAIL and TEST_USER_PASSWORD in .env');

    test('the control remains available with equivalent behavior when logged in', async ({ page, authUser }) => {
      await page.goto('/');
      await expect(whatsappButton(page)).toBeVisible();
    });
  });

  test('a valid handoff opens a window, isolates opener, and shows local-only never-claims-delivery feedback', async ({ page, homePage }) => {
    await homePage.open('/');
    await stubWindowOpen(page);
    await whatsappButton(page).click();

    const status = page.locator('[role="status"].whatsapp-feedback');
    await expect(status).toHaveText('Se abrió el acceso a WhatsApp; completá el envío allí.');
    expect(await status.innerText()).not.toMatch(/enviad|entregad|leíd/i);

    const handoff = await page.evaluate(() => window.__handoff);
    expect(handoff.opened).toBe(true);
    expect(handoff.openerNulled).toBe(true);
    expect(handoff.location).toContain('https://wa.me/');
    expect(handoff.location).not.toContain('undefined');
  });

  test('a blocked popup (window.open returns null) reports an actionable failure without crashing', async ({ page, homePage }) => {
    await homePage.open('/');
    await stubWindowOpen(page, { returnsNull: true });
    await whatsappButton(page).click();

    const alert = page.locator('[role="alert"].whatsapp-feedback');
    await expect(alert).toHaveText(
      'No pudimos abrir WhatsApp. Habilitá las ventanas emergentes e intentá de nuevo.',
    );
    // The button remains available for a retry after a detectable failure.
    await expect(whatsappButton(page)).toBeEnabled();
  });

  test('a URL-assignment exception closes the blank window and reports the same actionable failure', async ({ page, homePage }) => {
    await homePage.open('/');
    await stubWindowOpen(page, { throwsOnAssign: true });
    await whatsappButton(page).click();

    const alert = page.locator('[role="alert"].whatsapp-feedback');
    await expect(alert).toBeVisible();
    const handoff = await page.evaluate(() => window.__handoff);
    expect(handoff.closed).toBe(true);
  });
});
