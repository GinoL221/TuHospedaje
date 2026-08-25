// @ts-check
const { test, expect } = require('../fixtures/fixtures');

test.describe('Search flow', () => {
  test('searching by city stays on Home with city query', async ({ page, homePage }) => {
    await homePage.open('/');
    await homePage.searchByCity('Buenos Aires');
    await expect(page).toHaveURL(/\/\?.*city=/);
    await expect(page.getByText(/resultados de/)).toBeVisible();
  });

  test('search results page renders heading', async ({ page }) => {
    await page.goto('/search?city=Buenos+Aires');
    const heading = page.getByRole('heading');
    await expect(heading.first()).toBeVisible();
  });
});
