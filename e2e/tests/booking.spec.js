// @ts-check
const { test, expect } = require('../fixtures/fixtures');

/**
 * Far-future ISO dates avoid collisions with seed/demo reservations.
 * Typed into react-datepicker inputs (dd/MM/yyyy).
 */
const CHECK_IN = '10/11/2099';
const CHECK_OUT = '13/11/2099';
const GUEST_PHONE = '+54 11 5555 0199';

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
    await page.goto('/lodgings/1');
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible();

    await setDatePickerValue(page, 'product-check-in', CHECK_IN);
    await setDatePickerValue(page, 'product-check-out', CHECK_OUT);

    const reserve = page.getByRole('button', { name: 'Reservar' });
    await expect(reserve).toBeEnabled();
    await reserve.click();

    await expect(page).toHaveURL(/\/booking\/1/);
    await expect(page.getByRole('heading', { name: 'Confirmar reserva' })).toBeVisible();

    await page.locator('#booking-phone').fill(GUEST_PHONE);
    await expect(page.locator('#booking-check-in')).toHaveValue(CHECK_IN);
    await expect(page.locator('#booking-check-out')).toHaveValue(CHECK_OUT);

    await page.getByRole('button', { name: 'Confirmar reserva' }).click();

    await expect(page).toHaveURL(/\/booking\/confirmation/);
    await expect(page.getByRole('heading', { name: '¡Reserva confirmada!' })).toBeVisible();
    await expect(page.getByText(`${CHECK_IN} → ${CHECK_OUT}`)).toBeVisible();

    await page.getByRole('link', { name: 'Ver mis reservas' }).click();
    await expect(page).toHaveURL(/\/my-reservations/);
    await expect(page.getByText(`${CHECK_IN} → ${CHECK_OUT}`)).toBeVisible();
    await expect(page.getByText('CONFIRMED')).toBeVisible();
    await expect(page.getByRole('link', { name: /Ver alojamiento/ })).toBeVisible();
  });
});
