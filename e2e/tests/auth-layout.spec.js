// @ts-check
const { test, expect } = require("@playwright/test");

for (const route of ["/login", "/register"]) {
  for (const width of [1920, 769, 768, 390]) {
    test(`${route} keeps the form centered and within the viewport at ${width}px`, async ({
      page,
    }) => {
      await page.setViewportSize({ width, height: 1080 });
      await page.goto(route);
      const card = page.locator(".login-box, .register-box");
      await expect(card).toBeVisible();
      await expect
        .poll(() =>
          card.evaluate((element) => {
            const { x, width } = element.getBoundingClientRect();
            return Math.abs(x + width / 2 - window.innerWidth / 2);
          }),
        )
        .toBeLessThanOrEqual(2);

      const bounds = await card.boundingBox();
      if (!bounds) throw new Error("Expected a visible auth card");
      expect(bounds.x).toBeGreaterThanOrEqual(0);
      expect(bounds.x + bounds.width).toBeLessThanOrEqual(width);
      expect(
        await page.evaluate(
          () =>
            document.documentElement.scrollWidth -
            document.documentElement.clientWidth,
        ),
      ).toBeLessThanOrEqual(0);
    });
  }
}
