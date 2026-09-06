// @ts-check
const { test, expect } = require('../fixtures/fixtures');

const LODGING_ID = 6;

const lodging = {
  id: LODGING_ID,
  name: 'Hotel Internacional',
  city: 'Buenos Aires',
  country: 'Argentina',
  pricePerNight: 120,
  imageUrls: [],
  description: 'Alojamiento de prueba para la cobertura mobile.',
  features: [],
};

/** @param {import('@playwright/test').Locator} locator */
async function expectTouchTarget(locator) {
  const box = await locator.boundingBox();
  expect(box).not.toBeNull();
  expect(box.width).toBeGreaterThanOrEqual(44);
  expect(box.height).toBeGreaterThanOrEqual(44);
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

/** @param {import('@playwright/test').Page} page */
async function expectNoHorizontalOverflow(page) {
  const documentSize = await page.evaluate(() => ({
    clientWidth: document.documentElement.clientWidth,
    scrollWidth: document.documentElement.scrollWidth,
  }));

  expect(documentSize.scrollWidth).toBeLessThanOrEqual(documentSize.clientWidth);
}

/** @param {import('@playwright/test').Page} page */
async function interceptBookingApi(page) {
  await page.route(`**/lodgings/${LODGING_ID}`, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(lodging),
    });
  });
  await page.route('**/reservations/my', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: '[]',
    });
  });
  await page.route(`**/lodgings/${LODGING_ID}/availability**`, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ occupiedRanges: [], available: true }),
    });
  });
}

/** @param {import('@playwright/test').Page} page */
async function exerciseBooking(page) {
  await interceptBookingApi(page);
  await page.goto(`/booking/${LODGING_ID}`);

  await expect(page.getByRole('heading', { name: 'Confirmar reserva' })).toBeVisible();
  await expect(page.getByText('Todas las fechas están disponibles.')).toBeVisible();

  const checkIn = page.locator('#booking-check-in');
  const checkOut = page.locator('#booking-check-out');
  const guestToggle = page.getByRole('button', { name: 'Ocultar detalles del huésped' });
  const submit = page.getByRole('button', { name: 'Confirmar reserva' });

  for (const control of [checkIn, checkOut, guestToggle, submit]) {
    await expectTouchTarget(control);
    await control.scrollIntoViewIfNeeded();
    await expectContainedInViewport(page, control);
  }

  const phone = page.locator('#booking-phone');
  await expect(phone).toBeVisible();
  await expectTouchTarget(phone);
  await phone.fill('+541100000001');
  await guestToggle.click();
  await expect(page.getByRole('button', { name: 'Mostrar detalles del huésped' })).toHaveAttribute(
    'aria-expanded',
    'false',
  );
  await page.getByRole('button', { name: 'Mostrar detalles del huésped' }).click();
  await expect(phone).toBeVisible();
  await expect(page.getByRole('button', { name: 'Ocultar detalles del huésped' })).toHaveAttribute(
    'aria-expanded',
    'true',
  );
  await expectContainedInViewport(page, phone);

  await checkIn.click();
  const popper = page.locator('.react-datepicker-popper').last();
  const calendar = popper.locator('.react-datepicker');
  await expect(calendar).toBeVisible();
  await expectContainedInViewport(page, calendar);
  await expectTouchTarget(popper.locator('.react-datepicker__navigation--next'));
  await expectContainedInViewport(page, popper.locator('.react-datepicker__navigation--next'));
  await page.keyboard.press('Escape');

  await expectNoHorizontalOverflow(page);
}

test.describe('Mobile Booking', () => {
  test.skip(!process.env.TEST_USER_EMAIL, 'Set TEST_USER_EMAIL and TEST_USER_PASSWORD in .env');

  test('keeps the booking form usable at the project mobile viewport', async ({ page, authUser }) => {
    await exerciseBooking(page);
  });
});

test.describe('Mobile Booking at 320x844', () => {
  test.skip(!process.env.TEST_USER_EMAIL, 'Set TEST_USER_EMAIL and TEST_USER_PASSWORD in .env');
  test.use({ viewport: { width: 320, height: 844 } });

  test('keeps the booking form usable without overflow', async ({ page, authUser }) => {
    await exerciseBooking(page);
  });
});
