// @ts-check
const { test, expect } = require('../fixtures/fixtures');

const longReservation = {
  id: 9001,
  lodgingId: 10,
  lodgingName: 'Alojamiento con un nombre excepcionalmente largo para pantallas pequeñas',
  city: 'San Carlos de Bariloche',
  status: 'CONFIRMED',
  checkIn: '2099-07-01',
  checkOut: '2099-07-31',
  guestName: 'Test User',
  guestEmail: 'persona.con.un.correo.muy.largo@subdominio.example.com',
  guestPhone: '+54 9 11 5555 1234 9876',
  totalPrice: 1234567,
};

async function expectNoHorizontalOverflow(page) {
  const viewport = await page.evaluate(() => ({
    clientWidth: document.documentElement.clientWidth,
    scrollWidth: document.documentElement.scrollWidth,
  }));

  expect(viewport.scrollWidth).toBeLessThanOrEqual(viewport.clientWidth);
}

/** @param {import('@playwright/test').Locator} locator */
async function expectTouchTarget(locator) {
  const box = await locator.boundingBox();
  expect(box).not.toBeNull();
  expect(box.width).toBeGreaterThanOrEqual(44);
  expect(box.height).toBeGreaterThanOrEqual(44);
}

async function useLongReservationHarness(page) {
  await page.route('**/reservations/my', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([longReservation]),
    });
  });
  await page.route('**/reservations/9001/cancel', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ ...longReservation, status: 'CANCELLED' }),
    });
  });
  page.on('dialog', (dialog) => dialog.accept());
}

async function exerciseReservations(page) {
  await useLongReservationHarness(page);
  await page.goto('/my-reservations');

  const card = page.locator('.reservation-card');
  await expect(card).toBeVisible();
  await expect(page.getByText(longReservation.lodgingName)).toBeVisible();
  await expect(page.getByText(longReservation.guestEmail)).toBeVisible();
  await expect(page.getByText(longReservation.guestPhone)).toBeVisible();
  await expect(page.getByText('01/07/2099 → 31/07/2099')).toBeVisible();
  await expect(page.getByText('30 noches')).toBeVisible();
  await expect(page.getByText('CONFIRMED')).toBeVisible();
  await expect(page.getByText(/\$1[.,]234[.,]567/)).toBeVisible();

  const lodgingLink = page.getByRole('link', { name: /Ver alojamiento/ });
  const cancelButton = page.getByRole('button', { name: 'Cancelar reserva' });
  await expectTouchTarget(lodgingLink);
  await expectTouchTarget(cancelButton);
  await lodgingLink.scrollIntoViewIfNeeded();
  await cancelButton.scrollIntoViewIfNeeded();
  await expectNoHorizontalOverflow(page);

  await cancelButton.click();
  await expect(page.getByText('CANCELLED')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Cancelar reserva' })).toHaveCount(0);
  await expectNoHorizontalOverflow(page);

  for (const socialLink of await page.locator('.footer-right a').all()) {
    await expectTouchTarget(socialLink);
    await socialLink.focus();
    await expect(socialLink).toBeFocused();
    expect(await socialLink.evaluate((element) => getComputedStyle(element).outlineWidth)).toBe('3px');
  }
}

test.describe('Mobile reservations', () => {
  test.skip(!process.env.TEST_USER_EMAIL, 'Set TEST_USER_EMAIL and TEST_USER_PASSWORD in .env');

  test.describe('at 320px', () => {
    test.use({ viewport: { width: 320, height: 844 } });

    test('keeps reservation content and actions reachable without overflow', async ({ page, authUser }) => {
      await exerciseReservations(page);
    });
  });

  test.describe('at 390px', () => {
    test.use({ viewport: { width: 390, height: 844 } });

    test('keeps reservation content and actions reachable without overflow', async ({ page, authUser }) => {
      await exerciseReservations(page);
    });
  });
});
