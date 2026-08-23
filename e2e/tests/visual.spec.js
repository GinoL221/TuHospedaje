// @ts-check
const { test, expect } = require('../fixtures/fixtures');

// Images from Unsplash are masked to avoid flakiness from external CDN variance.
const maskImages = (page) => [page.locator('img')];

// A 1x1 PNG fulfilled instantly for every non-local image request (avatar,
// footer social icons, lodging/category photos). maskImages() only covers
// whatever <img> already exists in the DOM at capture time — without this,
// a real network round-trip to an external host can still be in flight when
// the screenshot is taken, so the real (unmasked) image occasionally renders
// in time and the snapshot no longer matches. Fulfilling locally removes
// that race entirely; it changes nothing about what the real app does.
const BLANK_PNG = Buffer.from(
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAIAAACQd1PeAAAADElEQVR4nGM4ceIEAAS0AlkWLoFAAAAAAElFTkSuQmCC',
  'base64',
);

/** Fixed payload so my-reservations screenshots do not depend on seed or ephemeral users. */
const visualReservation = {
  id: 9100,
  lodgingId: 1,
  lodgingName: 'Hotel Buenos Aires Centro',
  city: 'Buenos Aires',
  status: 'CONFIRMED',
  checkIn: '2099-03-10',
  checkOut: '2099-03-14',
  guestName: 'E2E Visual',
  guestEmail: 'e2e.visual@tuhospedaje.com',
  guestPhone: '+54 11 4000 0000',
  totalPrice: 48000,
};

async function useMyReservationsHarness(page) {
  await page.route('**/reservations/my', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([visualReservation]),
    });
  });
}

test.describe('Visual regression', () => {
  test.beforeEach(async ({ page }) => {
    await page.route(
      (url) => url.hostname !== 'localhost',
      (route) => {
        if (route.request().resourceType() === 'image') {
          return route.fulfill({ body: BLANK_PNG, contentType: 'image/png' });
        }
        return route.continue();
      },
    );
  });

  test('home page', async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveScreenshot('home.png', {
      mask: maskImages(page),
    });
  });

  test('login page', async ({ page }) => {
    await page.goto('/login');
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveScreenshot('login.png');
  });

  test('register page', async ({ page }) => {
    await page.goto('/register');
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveScreenshot('register.png');
  });

  test('search results page', async ({ page }) => {
    await page.goto('/search?city=Buenos+Aires');
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveScreenshot('search-results.png', {
      mask: maskImages(page),
    });
  });

  test('lodging detail page', async ({ page }) => {
    await page.goto('/lodgings/1');
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveScreenshot('lodging-detail.png', {
      mask: maskImages(page),
    });
  });

  test.skip(!process.env.TEST_USER_EMAIL, 'Set TEST_USER_EMAIL and TEST_USER_PASSWORD in .env');

  test('my reservations page (authenticated)', async ({ page, authUser }) => {
    await useMyReservationsHarness(page);
    await page.goto('/my-reservations');
    await page.waitForLoadState('networkidle');
    await expect(page.getByText(visualReservation.lodgingName)).toBeVisible();
    await expect(page).toHaveScreenshot('my-reservations.png', {
      mask: maskImages(page),
    });
  });
});
