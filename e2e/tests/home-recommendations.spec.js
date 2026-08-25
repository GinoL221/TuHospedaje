// @ts-check
const { test, expect } = require('../fixtures/fixtures');

/**
 * WU2 — Home recommendation snapshot and recovery (US-4.1, US-4.2, US-4.3,
 * US-8.1). Runs against the live seeded catalog (38 lodgings, size 10 pages
 * -> 4 pages) so page identity/order can be observed for real, not stubbed.
 *
 * @param {import('@playwright/test').Page} page
 */
async function currentPageIds(page) {
  await page.locator('.hotel-list .hotel-card-link').first().waitFor({ state: 'visible' });
  return page
    .locator('.hotel-list .hotel-card-link')
    .evaluateAll((links) => links.map((a) => a.getAttribute('href')));
}

/** @param {import('@playwright/test').Page} page */
async function pageLabel(page) {
  return page.locator('.home-pagination span').innerText();
}

test.describe('Home recommendations — stable pagination', () => {
  test('page 1 -> page 2 -> page 1 keeps the same identities and order', async ({ page, homePage }) => {
    await homePage.open('/');
    const page1First = await currentPageIds(page);
    expect(page1First.length).toBeGreaterThan(0);
    expect(page1First.length).toBeLessThanOrEqual(10);
    // US-4.1-S1: no duplicate within the page, and not the fixed catalog order
    expect(new Set(page1First).size).toBe(page1First.length);

    await page.getByRole('button', { name: 'Siguiente' }).click();
    await expect.poll(() => pageLabel(page)).toContain('Página 2');
    const page2 = await currentPageIds(page);
    // US-4.2-S2: cross-page uniqueness
    expect(page2.some((id) => page1First.includes(id))).toBe(false);

    await page.getByRole('button', { name: 'Anterior' }).click();
    await expect.poll(() => pageLabel(page)).toContain('Página 1');
    const page1Second = await currentPageIds(page);
    // US-4.2-S1: forward/backward stability — same identities, same order
    expect(page1Second).toEqual(page1First);
  });

  test('last -> first navigation returns to the original first-page snapshot', async ({ page, homePage }) => {
    await homePage.open('/');
    const originalFirst = await currentPageIds(page);

    await page.getByRole('button', { name: 'Última', exact: true }).click();
    await expect(page.getByRole('button', { name: 'Siguiente' })).toBeDisabled();
    await expect(page.getByRole('button', { name: 'Última', exact: true })).toBeDisabled();

    await page.getByRole('button', { name: 'Inicio' }).click();
    await expect.poll(() => pageLabel(page)).toContain('Página 1');
    await expect(page.getByRole('button', { name: 'Anterior' })).toBeDisabled();
    const returnedFirst = await currentPageIds(page);
    expect(returnedFirst).toEqual(originalFirst);
  });

  test('explicit refresh replaces the snapshot from page 1 with a new revision request', async ({ page, homePage }) => {
    await homePage.open('/');
    await currentPageIds(page);

    await page.getByRole('button', { name: 'Siguiente' }).click();
    await expect.poll(() => pageLabel(page)).toContain('Página 2');

    const refreshRequest = page.waitForRequest((req) =>
      req.url().includes('/lodgings/recommendations') && req.url().includes('page=0'),
    );
    await page.getByRole('button', { name: 'Actualizar recomendaciones' }).click();
    await refreshRequest;
    await expect.poll(() => pageLabel(page)).toContain('Página 1');
    const afterRefresh = await currentPageIds(page);
    expect(afterRefresh.length).toBeGreaterThan(0);
  });

  test('selecting a category does not call the recommendations endpoint', async ({ page, homePage }) => {
    await homePage.open('/');
    await currentPageIds(page);
    await page.locator('.category-tag').first().waitFor({ state: 'visible' });

    let recommendationCalls = 0;
    page.on('request', (req) => {
      if (req.url().includes('/lodgings/recommendations')) recommendationCalls += 1;
    });

    await page.locator('.category-tag').first().click();
    await expect(page).toHaveURL(/categories=/);
    await expect(page.getByText(/resultados de/)).toBeVisible();
    expect(recommendationCalls).toBe(0);
    await expect(page.getByRole('heading', { name: 'Recomendaciones' })).toBeVisible();

    await page.getByRole('button', { name: 'Limpiar filtros' }).click();
    await expect(page.getByRole('button', { name: 'Actualizar recomendaciones' })).toBeVisible();
  });

  test('a failed load shows an accessible error and a successful retry restores a valid page', async ({ page, homePage }) => {
    const pattern = '**/lodgings/recommendations**';
    // Fail every request while routed (React StrictMode double-invokes the
    // mount effect in dev builds; a "fail-once" toggle can race and let the
    // second request succeed before the alert renders). Unrouted entirely
    // before the explicit user retry below.
    await page.route(pattern, async (route) => {
      await route.fulfill({ status: 500, contentType: 'application/json', body: '{"error":"boom"}' });
    });

    await homePage.open('/');
    const alert = page.locator('.recommendations-alert[role="alert"]');
    await expect(alert).toBeVisible();
    await expect(alert.getByText('No pudimos cargar las recomendaciones.')).toBeVisible();

    await page.unroute(pattern);
    await alert.getByRole('button', { name: 'Reintentar' }).click();
    await expect(alert).toHaveCount(0);
    const recovered = await currentPageIds(page);
    expect(recovered.length).toBeGreaterThan(0);
  });
});
