// @ts-check
const { test, expect } = require("@playwright/test");

const lodging = {
  id: 6,
  name: "Hotel de prueba local",
  city: "Buenos Aires",
  country: "Argentina",
  pricePerNight: 120,
  imageUrls: Array.from(
    { length: 5 },
    (_, index) => `/src/assets/images/hotel${index + 1}.jpg`,
  ),
  description: "Alojamiento local para verificar la galería.",
  features: [],
};

async function mockLodging(page) {
  await page.route("**/api/lodgings/6", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(lodging),
    }),
  );
  await page.route("**/api/lodgings/6/availability**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ occupiedRanges: [] }),
    }),
  );
}

test("gallery keeps the 1024px desktop split and existing 900px and mobile reflow", async ({
  page,
}) => {
  await mockLodging(page);
  await page.goto("/lodgings/6");
  await expect(page.locator(".gallery-wrapper")).toBeVisible();

  await page.setViewportSize({ width: 1024, height: 768 });
  const gallery = page.locator(".gallery-wrapper");
  const main = page.locator(".gallery-main");
  const aside = page.locator(".gallery-thumbs-col");
  await expect
    .poll(async () => {
      const [galleryBox, mainBox] = await Promise.all([
        gallery.boundingBox(),
        main.boundingBox(),
      ]);
      return Math.abs(mainBox.width - (galleryBox.width - 10) / 2);
    })
    .toBeLessThanOrEqual(2);
  const rowPositions = await page
    .locator(".gallery-thumb")
    .evaluateAll((elements) =>
      elements.map((element) => element.getBoundingClientRect().y),
    );
  expect(Math.abs(rowPositions[1] - rowPositions[0])).toBeLessThanOrEqual(2);
  expect(rowPositions[2]).toBeGreaterThan(rowPositions[0] + 2);
  expect(Math.abs(rowPositions[3] - rowPositions[2])).toBeLessThanOrEqual(2);

  await page.setViewportSize({ width: 900, height: 768 });
  await expect
    .poll(() =>
      gallery.evaluate((element) => getComputedStyle(element).display),
    )
    .toBe("flex");
  const [mainAt900, asideAt900] = await Promise.all([
    main.boundingBox(),
    aside.boundingBox(),
  ]);
  expect(mainAt900.width).toBeGreaterThan(asideAt900.width);

  await page.setViewportSize({ width: 768, height: 900 });
  await expect
    .poll(() =>
      gallery.evaluate((element) => getComputedStyle(element).flexDirection),
    )
    .toBe("column");
  await expect(page.locator(".gallery-mobile-controls")).toBeVisible();
  await expect
    .poll(() =>
      page
        .locator(".gallery-desktop-arrow")
        .evaluateAll((elements) =>
          elements.every(
            (element) => getComputedStyle(element).display === "none",
          ),
        ),
    )
    .toBe(true);
  const overflow = await page.evaluate(
    () =>
      document.documentElement.scrollWidth -
      document.documentElement.clientWidth,
  );
  expect(overflow).toBeLessThanOrEqual(0);
});

test("desktop gallery splits content evenly and lays out four thumbnails in a 2x2 grid", async ({
  page,
}) => {
  await page.setViewportSize({ width: 1366, height: 768 });
  await mockLodging(page);
  await page.goto("/lodgings/6");

  const gallery = page.locator(".gallery-wrapper");
  const main = page.locator(".gallery-main");
  const thumbs = page.locator(".gallery-thumb");
  await expect(gallery).toBeVisible();
  await expect(thumbs).toHaveCount(4);
  await expect
    .poll(() =>
      page
        .locator(".gallery-main-trigger img")
        .evaluate((image) => image.complete && image.naturalWidth > 0),
    )
    .toBe(true);

  const [galleryBox, mainBox, thumbBoxes] = await Promise.all([
    gallery.boundingBox(),
    main.boundingBox(),
    thumbs.evaluateAll((elements) =>
      elements.map((element) => {
        const { x, y, width, height } = element.getBoundingClientRect();
        return { x, y, width, height };
      }),
    ),
  ]);
  expect(galleryBox).not.toBeNull();
  expect(mainBox).not.toBeNull();
  expect(galleryBox.width).toBeGreaterThan(0);
  expect(
    Math.abs(mainBox.width - (galleryBox.width - 10) / 2),
  ).toBeLessThanOrEqual(2);
  expect(thumbBoxes).toHaveLength(4);
  for (const box of thumbBoxes) {
    expect(box.width).toBeGreaterThan(0);
    expect(box.height).toBeGreaterThan(0);
    expect(box.x).toBeGreaterThanOrEqual(mainBox.x + mainBox.width + 8);
    expect(box.x + box.width).toBeLessThanOrEqual(
      galleryBox.x + galleryBox.width + 2,
    );
    expect(box.y).toBeGreaterThanOrEqual(galleryBox.y - 2);
    expect(box.y + box.height).toBeLessThanOrEqual(
      galleryBox.y + galleryBox.height + 2,
    );
  }
  const firstRow = thumbBoxes.filter(
    (box) => Math.abs(box.y - thumbBoxes[0].y) <= 2,
  );
  const secondRow = thumbBoxes.filter((box) => box.y > thumbBoxes[0].y + 2);
  expect(firstRow).toHaveLength(2);
  expect(secondRow).toHaveLength(2);
  expect(Math.abs(firstRow[0].x - firstRow[1].x)).toBeGreaterThan(0);
  expect(Math.abs(secondRow[0].x - secondRow[1].x)).toBeGreaterThan(0);
});
