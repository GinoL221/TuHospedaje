// @ts-check
const { test, expect } = require('../fixtures/fixtures');

const favorites = [
  {
    id: 41,
    name: 'Cabaña del Lago con un nombre largo para pantallas pequeñas',
    city: 'San Carlos de Bariloche',
    country: 'Argentina',
    description: 'Una cabaña con vista al lago.',
    pricePerNight: 125000,
    imageUrls: [],
  },
  {
    id: 42,
    name: 'Departamento Centro',
    city: 'Mendoza',
    country: 'Argentina',
    description: 'Departamento céntrico.',
    pricePerNight: 95000,
    imageUrls: [],
  },
];

async function expectNoHorizontalOverflow(page) {
  const size = await page.evaluate(() => ({
    clientWidth: document.documentElement.clientWidth,
    scrollWidth: document.documentElement.scrollWidth,
  }));
  expect(size.scrollWidth).toBeLessThanOrEqual(size.clientWidth);
}

async function registerOrdinaryUser(page, registerPage) {
  const uniqueEmail = `e2e_favorites_${Date.now()}_${Math.random().toString(36).slice(2)}@test.com`;

  await registerPage.open('/register');
  const registerResponse = page.waitForResponse((response) =>
    response.request().method() === 'POST' && response.url().endsWith('/api/auth/register')
  );
  await registerPage.register({
    firstName: 'Favorites',
    lastName: 'E2E',
    email: uniqueEmail,
    password: 'Test1234',
  });
  expect((await registerResponse).status()).toBe(201);
  await page.waitForURL('/');
}

async function exerciseFavorites(page) {
  let firstDeleteAttempts = 0;
  let releaseFirstAttempt;
  const firstAttemptPending = new Promise((resolve) => {
    releaseFirstAttempt = resolve;
  });

  await page.route('**/api/favorites', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(favorites) });
  });
  await page.route('**/api/favorites/41', async (route) => {
    firstDeleteAttempts += 1;
    if (firstDeleteAttempts === 1) {
      await firstAttemptPending;
      await route.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify({ message: 'No se pudo quitar este favorito.' }) });
      return;
    }
    await route.fulfill({ status: 204 });
  });
  await page.route('**/api/favorites/42', async (route) => {
    await route.fulfill({ status: 204 });
  });

  await page.goto('/favorites');
  await expect(page.getByRole('heading', { name: 'Mis favoritos' })).toBeVisible();
  await expectNoHorizontalOverflow(page);

  const firstItem = page.locator('.favorite-item').filter({ hasText: favorites[0].name });
  const secondItem = page.locator('.favorite-item').filter({ hasText: favorites[1].name });
  const removeButton = firstItem.getByRole('button', { name: 'Quitar de favoritos' });
  const box = await removeButton.boundingBox();
  expect(box).not.toBeNull();
  expect(box.width).toBeGreaterThanOrEqual(44);
  expect(box.height).toBeGreaterThanOrEqual(44);
  await removeButton.focus();
  await expect(removeButton).toBeFocused();
  expect(await removeButton.evaluate((element) => getComputedStyle(element).outlineWidth)).not.toBe('0px');

  await removeButton.click();
  const pendingRemoveButton = firstItem.getByRole('button', { name: 'Quitando de favoritos' });
  await expect(pendingRemoveButton).toBeDisabled();
  await pendingRemoveButton.click({ force: true });
  expect(firstDeleteAttempts).toBe(1);
  releaseFirstAttempt();

  await expect(firstItem.getByRole('alert')).toHaveText('Error 503');
  await expect(firstItem).toBeVisible();
  await expect(secondItem).toBeVisible();

  await firstItem.getByRole('button', { name: 'Quitar de favoritos' }).click();
  await expect(firstItem).toHaveCount(0);
  await expect(secondItem).toBeVisible();
  expect(firstDeleteAttempts).toBe(2);
  await expectNoHorizontalOverflow(page);
}

test.describe('Mobile Favorites', () => {
  test('supports item-scoped removal at the project mobile viewport', async ({ page, registerPage }) => {
    await registerOrdinaryUser(page, registerPage);
    await exerciseFavorites(page);
  });

  test.describe('at 320x844', () => {
    test.use({ viewport: { width: 320, height: 844 } });

    test('supports item-scoped removal without horizontal overflow', async ({ page, registerPage }) => {
      await registerOrdinaryUser(page, registerPage);
      await exerciseFavorites(page);
    });
  });
});
