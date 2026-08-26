// @ts-check
const { test, expect } = require('../fixtures/fixtures');

/**
 * WU6/WU7 — Shared availability state machine, ProductDetail wiring, and
 * Booking preflight/conflict recovery (US-23.1, US-23.2). Uses route
 * interception on the availability endpoint to force each observable
 * state deterministically; the reservation-conflict scenario simulates the
 * backend's final-authority overlap rejection without racing real data.
 */

const LODGING_ID = 6; // Hotel Internacional — not used by the reviews fixture reservation.

test.describe('ProductDetail — availability states', () => {
  test.skip(!process.env.TEST_USER_EMAIL, 'Set TEST_USER_EMAIL and TEST_USER_PASSWORD in .env');

  test('shows loading then a ready usable state with zero occupied ranges', async ({ page, authUser }) => {
    await page.route(`**/lodgings/${LODGING_ID}/availability**`, async (route) => {
      await new Promise((r) => setTimeout(r, 800));
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ occupiedRanges: [] }),
      });
    });

    await page.goto(`/lodgings/${LODGING_ID}`);
    await expect(page.locator('.availability-status[role="status"]')).toHaveText(
      'Comprobando disponibilidad...',
    );
    await expect(page.locator('.availability-status[role="status"]')).toHaveText(
      'Todas las fechas están disponibles.',
    );
    await expect(page.locator('#product-check-in')).toBeEnabled();
  });

  test('occupied ranges disable those dates and reserve stays blocked until a full valid range is chosen', async ({ page, authUser }) => {
    const today = new Date();
    const occupiedStart = new Date(today.getFullYear(), today.getMonth() + 1, 10);
    const occupiedEnd = new Date(today.getFullYear(), today.getMonth() + 1, 15);
    await page.route(`**/lodgings/${LODGING_ID}/availability**`, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          occupiedRanges: [
            {
              checkIn: occupiedStart.toISOString().split('T')[0],
              checkOut: occupiedEnd.toISOString().split('T')[0],
            },
          ],
        }),
      });
    });

    await page.goto(`/lodgings/${LODGING_ID}`);
    await expect(page.locator('.availability-status[role="status"]')).toHaveCount(0);
    await expect(page.getByRole('button', { name: 'Reservar' })).toBeDisabled();
  });

  test('initial failure shows an accessible alert and retry recovers a ready state', async ({ page, authUser }) => {
    const pattern = `**/lodgings/${LODGING_ID}/availability**`;
    // Fail every request (React StrictMode double-invokes the mount effect
    // in dev builds, so a "fail-once-then-pass" toggle can race and let the
    // second, superseding request succeed before the alert ever renders).
    // The interception is removed entirely before the explicit user retry.
    await page.route(pattern, async (route) => {
      await route.fulfill({ status: 500, contentType: 'application/json', body: '{"error":"boom"}' });
    });

    await page.goto(`/lodgings/${LODGING_ID}`);
    const alert = page.locator('.availability-alert[role="alert"]');
    await expect(alert).toBeVisible();
    await expect(alert.getByText('No pudimos obtener la disponibilidad de este alojamiento.')).toBeVisible();

    await page.unroute(pattern);
    await alert.getByRole('button', { name: 'Reintentar' }).click();
    await expect(alert).toHaveCount(0);
    await expect(page.locator('.availability-status[role="status"]')).toHaveText(
      'Todas las fechas están disponibles.',
    );
  });
});

test.describe('BookingPage — preflight and conflict recovery', () => {
  test.skip(!process.env.TEST_USER_EMAIL, 'Set TEST_USER_EMAIL and TEST_USER_PASSWORD in .env');

  /** @param {import('@playwright/test').Page} page */
  async function readyRoute(page) {
    // `available: true` is required for the dated (checkIn/checkOut) client
    // preflight request to pass — the extra field is harmless on the
    // dateless (mount) request, which the hook ignores.
    await page.route(`**/lodgings/${LODGING_ID}/availability**`, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ occupiedRanges: [], available: true }),
      });
    });
  }

  /**
   * Opens the DatePicker for `pickerId` and clicks the visible day cell for
   * `date`, navigating forward month-by-month if the target month is not
   * the one currently displayed (bounded to avoid an infinite loop).
   * @param {import('@playwright/test').Page} page
   * @param {string} pickerId
   * @param {Date} date
   */
  async function selectDate(page, pickerId, date) {
    await page.locator(`#${pickerId}`).click();
    const popper = page.locator('.react-datepicker-popper').last();
    const targetLabel = date.toLocaleDateString('en-US', { month: 'long', year: 'numeric' });
    for (let attempt = 0; attempt < 12; attempt += 1) {
      const header = (await popper.locator('.react-datepicker__current-month').innerText()).toLowerCase();
      if (header.includes(targetLabel.toLowerCase())) break;
      await popper.locator('.react-datepicker__navigation--next').click();
    }
    await expect(popper.locator('.react-datepicker__current-month')).toHaveText(
      new RegExp(targetLabel, 'i'),
    );
    await popper
      .locator('.react-datepicker__day:not(.react-datepicker__day--outside-month)')
      .filter({ hasText: new RegExp(`^${date.getDate()}$`) })
      .first()
      .click();
  }

  /** @param {import('@playwright/test').Page} page */
  async function revealGuestPhone(page) {
    const showDetails = page.getByRole('button', { name: 'Mostrar detalles del huésped' });
    const hideDetails = page.getByRole('button', { name: 'Ocultar detalles del huésped' });
    await expect(showDetails.or(hideDetails)).toBeVisible();
    if (await showDetails.isVisible()) {
      await showDetails.click();
    }
    await expect(page.locator('#booking-phone')).toBeVisible();
  }

  /** @param {import('@playwright/test').Page} page */
  async function pickDates(page) {
    const today = new Date();
    const checkIn = new Date(today.getFullYear(), today.getMonth(), today.getDate() + 5);
    const checkOut = new Date(today.getFullYear(), today.getMonth(), today.getDate() + 8);
    await selectDate(page, 'booking-check-in', checkIn);
    await selectDate(page, 'booking-check-out', checkOut);
  }

  test('submit stays blocked until availability is ready, then a preflight conflict shows an inline alert with no navigation', async ({ page, authUser }) => {
    await page.route(`**/lodgings/${LODGING_ID}/availability**`, async (route, request) => {
      const url = new URL(request.url());
      const isDatedPreflight = url.searchParams.has('checkIn');
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(
          isDatedPreflight
            ? { occupiedRanges: [], available: false }
            : { occupiedRanges: [] },
        ),
      });
    });

    await page.goto(`/booking/${LODGING_ID}`);
    const submit = page.getByRole('button', { name: 'Confirmar reserva' });
    await expect(submit).toBeDisabled();
    await revealGuestPhone(page);
    await page.locator('#booking-phone').fill('+541100000001');

    await pickDates(page);
    await expect(page.locator('.availability-status[role="status"]')).toHaveText(
      'Todas las fechas están disponibles.',
    );
    await submit.click();

    const alert = page.locator('p.error[role="alert"]');
    await expect(alert).toHaveText('Las fechas seleccionadas ya no están disponibles. Elegí otro rango.');
    expect(page.url()).toContain(`/booking/${LODGING_ID}`);
  });

  test('a backend conflict on submit is the final authority and recovers availability for retry', async ({ page, authUser }) => {
    await readyRoute(page);
    await page.goto(`/booking/${LODGING_ID}`);
    await revealGuestPhone(page);
    await page.locator('#booking-phone').fill('+541100000002');
    await pickDates(page);
    await expect(page.locator('.availability-status[role="status"]')).toHaveText(
      'Todas las fechas están disponibles.',
    );

    // Preflight (dated GET) still reports available; only the POST is rejected,
    // simulating a race the client-side preflight could not see.
    await page.route('**/reservations', async (route, request) => {
      if (request.method() !== 'POST') return route.continue();
      await route.fulfill({
        status: 409,
        contentType: 'application/json',
        body: JSON.stringify({ error: 'El rango de fechas ya no está disponible.' }),
      });
    });

    const retryRequest = page.waitForRequest(
      (req) => req.url().includes(`/lodgings/${LODGING_ID}/availability`) && req.url().includes('checkIn'),
    );
    await page.getByRole('button', { name: 'Confirmar reserva' }).click();

    await expect(page.locator('p.error[role="alert"]')).toHaveText('El rango de fechas ya no está disponible.');
    await retryRequest; // retryAvailability() recovery refresh fired
    expect(page.url()).toContain(`/booking/${LODGING_ID}`);
  });
});
