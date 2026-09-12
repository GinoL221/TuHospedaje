// @ts-check
const { test, expect } = require('@playwright/test');

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
async function interceptBookingApi(page, { failFirstLodging = false } = {}) {
  let lodgingRequests = 0;
  let lodgingAvailable = !failFirstLodging;
  const json = (body, status = 200, headers = {}) => ({
    status,
    contentType: 'application/json',
    headers,
    body: JSON.stringify(body),
  });

  await page.route('**/auth/me', (route) =>
    route.fulfill(json({
      id: 42,
      firstName: 'Mobile',
      lastName: 'Tester',
      email: 'mobile@example.com',
    })),
  );
  await page.route('**/auth/csrf', (route) =>
    route.fulfill(json({}, 200, { 'set-cookie': 'XSRF-TOKEN=mobile-csrf; Path=/' })),
  );
  await page.route(`**/lodgings/${LODGING_ID}`, async (route) => {
    lodgingRequests += 1;
    if (!lodgingAvailable) {
      await route.fulfill(json({ error: 'Unavailable' }, 503));
      return;
    }
    await route.fulfill(json(lodging));
  });
  await page.route('**/reservations/my', (route) => route.fulfill(json([])));
  await page.route(`**/lodgings/${LODGING_ID}/availability**`, (route) =>
    route.fulfill(json({ occupiedRanges: [], available: true })),
  );
  await page.route('**/reservations', (route) =>
    route.fulfill(json({ id: 91, lodgingId: LODGING_ID })),
  );

  return {
    allowLodging: () => { lodgingAvailable = true; },
    lodgingRequestCount: () => lodgingRequests,
  };
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

  await checkIn.tap();
  const popper = page.locator('.react-datepicker-popper').last();
  const calendar = popper.locator('.react-datepicker');
  await expect(calendar).toBeVisible();
  await calendar.scrollIntoViewIfNeeded();
  await expectContainedInViewport(page, calendar);
  const nextMonth = popper.locator('.react-datepicker__navigation--next');
  await expectTouchTarget(nextMonth);
  await nextMonth.scrollIntoViewIfNeeded();
  await expectContainedInViewport(page, nextMonth);
  await page.keyboard.press('Escape');
  await checkIn.focus();
  await page.keyboard.press('Enter');
  await expect(calendar).toBeVisible();
  await page.keyboard.press('Escape');

  await expectNoHorizontalOverflow(page);
}

for (const viewport of [
  { width: 390, height: 844 },
  { width: 320, height: 844 },
]) {
  test.describe(`Mobile Booking at ${viewport.width}x${viewport.height}`, () => {
    test.use({ viewport, hasTouch: true });

    test('keeps the booking form and calendar usable without overflow', async ({ page }) => {
      await exerciseBooking(page);
    });
  });
}

test.describe('Mobile Booking root retry', () => {
  test.use({ viewport: { width: 320, height: 844 }, hasTouch: true });

  test('recovers from a lodging failure using the keyboard retry', async ({ page }) => {
    const requests = await interceptBookingApi(page, { failFirstLodging: true });
    await page.goto(`/booking/${LODGING_ID}`);

    await expect(page.getByRole('alert')).toContainText('No se pudo cargar el alojamiento.');
    const retry = page.getByRole('button', { name: 'Reintentar alojamiento' });
    await expectTouchTarget(retry);
    const requestsBeforeRetry = requests.lodgingRequestCount();
    requests.allowLodging();
    await retry.focus();
    await page.keyboard.press('Enter');

    await expect(page.getByRole('heading', { name: 'Confirmar reserva' })).toBeVisible();
    expect(requests.lodgingRequestCount()).toBe(requestsBeforeRetry + 1);
    await expectNoHorizontalOverflow(page);
  });
});
