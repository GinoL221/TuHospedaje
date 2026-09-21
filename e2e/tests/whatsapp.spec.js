// @ts-check
const { test, expect } = require("../fixtures/fixtures");

/**
 * WU10 — WhatsApp handoff feedback (US-34.1, US-34.2, US-34.3).
 * The declarative link opens WhatsApp in a separate browsing context; these
 * assertions verify its client-side contract and do not claim message delivery.
 * Invalid/missing configuration (US-34.2-S2) cannot be exercised here because
 * `VITE_WHATSAPP_NUMBER` is inlined at Vite build time, not readable at runtime;
 * that scenario is covered by
 * `frontend/src/components/WhatsAppButton/WhatsAppButton.test.jsx` (Vitest,
 * `vi.stubEnv`) — see the traceability matrix.
 */

const PRELOADED_MESSAGE =
  "Hola, quiero hacer una consulta sobre un alojamiento de TuHospedaje.";

/** @param {import('@playwright/test').Page} page */
function whatsappLink(page) {
  return page.getByRole("link", { name: "Contactar por WhatsApp" });
}

/** @param {ReturnType<typeof whatsappLink>} link */
async function expectDeclarativeWhatsAppContract(link) {
  await expect(link).toBeVisible();
  await expect(link).toHaveAttribute("target", "_blank");
  await expect(link).toHaveAttribute("rel", "noopener noreferrer");

  const href = await link.getAttribute("href");
  expect(href).not.toBeNull();

  const url = new URL(href);
  expect(url.protocol).toBe("https:");
  expect(url.hostname).toBe("wa.me");
  expect(url.port).toBe("");
  expect(url.username).toBe("");
  expect(url.password).toBe("");
  expect(url.pathname).toMatch(/^\/[1-9]\d{7,14}$/);
  expect(url.hash).toBe("");
  expect([...url.searchParams.keys()]).toEqual(["text"]);
  expect(url.searchParams.get("text")).toBe(PRELOADED_MESSAGE);
}

test.describe("WhatsApp handoff", () => {
  test("the declarative link has the secure WhatsApp contract for an anonymous visitor", async ({
    page,
    homePage,
  }) => {
    await homePage.open("/");

    await expectDeclarativeWhatsAppContract(whatsappLink(page));
  });

  test.describe("authenticated", () => {
    test.skip(
      !process.env.TEST_USER_EMAIL,
      "Requires CI-provided test-user credentials",
    );

    test("the declarative link has the equivalent secure contract when logged in", async ({
      page,
      authUser,
    }) => {
      await page.goto("/");

      await expectDeclarativeWhatsAppContract(whatsappLink(page));
    });
  });
});
