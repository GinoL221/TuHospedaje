// @ts-check
const { test, expect } = require('../fixtures/fixtures');

const GUEST_PHONE = '+54 11 5555 0199';

/** @param {Date} date */
function formatDisplayDate(date) {
  const day = String(date.getUTCDate()).padStart(2, '0');
  const month = String(date.getUTCMonth() + 1).padStart(2, '0');
  return `${day}/${month}/${date.getUTCFullYear()}`;
}

/**
 * Far-future stay unique per invocation so Playwright retries do not collide
 * with a reservation this test already created.
 */
function uniqueStay() {
  const checkIn = new Date(Date.UTC(2099, 0, 2 + (Date.now() % 300)));
  const checkOut = new Date(checkIn);
  checkOut.setUTCDate(checkOut.getUTCDate() + 3);
  return {
    checkIn: formatDisplayDate(checkIn),
    checkOut: formatDisplayDate(checkOut),
  };
}

/**
 * @param {import('@playwright/test').Page} page
 * @param {string} inputId
 * @param {string} displayDate dd/MM/yyyy
 */
async function setDatePickerValue(page, inputId, displayDate) {
  const input = page.locator(`#${inputId}`);
  await input.click();
  await input.fill(displayDate);
  await input.press('Enter');
  await page.keyboard.press('Escape');
}

test.describe('Booking — happy path', () => {
  test.skip(!process.env.TEST_USER_EMAIL, 'Set TEST_USER_EMAIL and TEST_USER_PASSWORD in .env');

  test('user books a lodging and sees it in my reservations', async ({ page, authUser }) => {
    const { checkIn, checkOut } = uniqueStay();
    const stayLabel = `${checkIn} → ${checkOut}`;

    await page.goto('/lodgings/1');
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible();

    await setDatePickerValue(page, 'product-check-in', checkIn);
    await setDatePickerValue(page, 'product-check-out', checkOut);

    const reserve = page.getByRole('button', { name: 'Reservar' });
    await expect(reserve).toBeEnabled();
    await reserve.click();

    await expect(page).toHaveURL(/\/booking\/1/);
    await expect(page.getByRole('heading', { name: 'Confirmar reserva' })).toBeVisible();

    await page.locator('#booking-phone').fill(GUEST_PHONE);
    await expect(page.locator('#booking-check-in')).toHaveValue(checkIn);
    await expect(page.locator('#booking-check-out')).toHaveValue(checkOut);

    await page.getByRole('button', { name: 'Confirmar reserva' }).click();

    await expect(page).toHaveURL(/\/booking\/confirmation/);
    await expect(page.getByRole('heading', { name: '¡Reserva confirmada!' })).toBeVisible();
    await expect(page.getByText(stayLabel)).toBeVisible();

    await page.getByRole('link', { name: 'Ver mis reservas' }).click();
    await expect(page).toHaveURL(/\/my-reservations/);

    const card = page.locator('article.reservation-card', { hasText: stayLabel });
    await expect(card).toBeVisible();
    await expect(card.locator('.reservation-status')).toHaveText('CONFIRMED');
    await expect(card.getByRole('link', { name: /Ver alojamiento/ })).toBeVisible();
  });
});
