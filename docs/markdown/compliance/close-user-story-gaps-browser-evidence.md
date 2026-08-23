# Browser Evidence — Close Original User-Story Gaps

**Change**: `close-user-story-gaps` | **Project**: tuhospedaje
**Date/build**: 2026-08-22 through 2026-08-23, committed WU1–WU11 stack, then follow-up `feat/home-search-ux`. Local browser evidence used the isolated stack: backend `dev` profile → MariaDB 10.11 container `tuhospedaje-dev-db` on host port **3307** (never 3306), frontend `npm run dev` on `:5173`, seed migration `V1_9000__dev_demo_data.sql` (6 categories, 38 lodgings, admin `admin@tuhospedaje.com` / `Admin1`). GitHub Actions verified the published PR #94 candidate on its Linux runner.
**Test runner**: Playwright `1.60.0`, bundled browser builds — Chromium and Firefox desktop projects (1280×720 default viewport unless noted), `mobile-chromium` project (Chromium engine, emulated viewports).
**Fixtures created for this WU11 pass** (see "Fixture setup" below for exact commands): a regular `USER` account `e2e.testuser@tuhospedaje.local`, and one directly-seeded `CONFIRMED` reservation (lodging 1, checkOut 2026-01-05, strictly before business today) proving rating eligibility without needing a real future-to-past booking flow.

Field set per row (adapted from design.md's per-scenario field list into fewer, denser columns for table readability — every named field is still present, just grouped): **Evidence ID · Browser/Viewport · Route · Role/Fixtures · Stubs/Preconditions · Steps → Expected → Observed · Screenshot/trace path · Overflow/keyboard/touch · Provider boundary · Result**.

## Fixture setup (real API/DB calls made once, not stubbed)

```
POST /api/auth/register {"firstName":"Test","lastName":"User","email":"e2e.testuser@tuhospedaje.local","password":"TestUser123"} → 201
# Direct insert into the isolated dev DB (docker exec tuhospedaje-dev-db mariadb ...), because no API path can
# create a CONFIRMED reservation with a checkout already in the past:
INSERT INTO reservations (check_in, check_out, total_price, lodging_id, user_id, version,
  guest_email, guest_name, guest_phone, status)
VALUES ('2026-01-01','2026-01-05', 600.00, 1, <user_id>, 0,
  'e2e.testuser@tuhospedaje.local', 'Test User', '+541100000000', 'CONFIRMED');
```
Verified once via `GET /api/ratings/lodging/1/eligibility` (authenticated as the fixture user) → `{"eligible":true,"reason":"ELIGIBLE"}`.

## Recommendations (US-4.1, US-4.2, US-4.3, US-8.1)

| ID | Browser/Viewport | Route | Role/Fixtures | Stubs/Preconditions | Steps → Expected → Observed | Screenshot/trace | Overflow/KB/Touch | Provider | Result |
|---|---|---|---|---|---|---|---|---|---|
| BE-REC-01 | Chromium desktop 1280×720 | `/` | anonymous; seeded 38-lodging catalog, tab-scoped `crypto.randomUUID()` seed | none (real backend recommendations endpoint) | Read page 1 (≤8 items after the follow-up `size=8` request, no dup, non-default order) → click Siguiente → assert page 2 has no overlap with page 1 → Anterior → assert page 1 identity unchanged (S1/S2). Click Última → assert Siguiente/Última disabled → Inicio → assert Anterior disabled and identity == original page 1 (US-8.1-S1). Click "Actualizar recomendaciones" → previous cards stay visible and busy until the new page arrives, then a fresh `page=0` request fires and the page label resets to "Página 1" (US-4.3-S1). Click a category tag → assert zero additional `/lodgings/recommendations` requests fire (category-filter compatibility). Route `**/lodgings/recommendations**` to fail once, then unroute → assert `.recommendations-alert[role=alert]` with Retry, then successful recovery (US-8.1-S3). | `test-results/home-recommendations-*` (Playwright auto-capture on failure only; all 5 sub-tests passed, so only trace-on-first-retry artifacts exist, none needed) | N/A (desktop) | N/A | **PASS** — 5/5 `chromium`, 5/5 `firefox` for the WU11 pagination/refresh contract. Follow-up `size=8` and keep-previous-cards behavior are covered by `Home.test.jsx`. |

## Administration (US-9.1, US-9.2)

| ID | Browser/Viewport | Route | Role/Fixtures | Stubs/Preconditions | Steps → Expected → Observed | Screenshot/trace | Overflow/KB/Touch | Provider | Result |
|---|---|---|---|---|---|---|---|---|---|
| BE-ADMIN-01 | Chromium + Firefox desktop | `/administración`, `/administraci%C3%B3n` (encoded), `/admin` (alias) | ADMIN (`admin@tuhospedaje.com`) | none — real login | Login → all 6 entity nav tabs visible (regression). Direct `page.goto('/administración')` → admin shell renders (US-9.1-S1). Direct `page.goto('/administraci%C3%B3n')` → same shell, no not-found (US-9.1-S2). Click every sidebar entity nav tab → each becomes active (regression). | `e2e/playwright-report/` (existing `admin-smoke.spec.js`, unmodified, executed live for the first time in this WU) | N/A | N/A | **PASS** — 3/3 `chromium`, 3/3 `firefox` |
| BE-ADMIN-02 | Mobile Chromium, 320×844, `hasTouch: true` | `/administración` (direct, post-login) | ADMIN | Login via plain form (not the `adminUser` fixture — that fixture itself asserts the desktop shell renders, which never happens under touch+narrow viewport, the exact condition under test) | Log in → navigate directly to `/administración` at 320×844 with touch emulated → assert `.admin-mobile-block[role="status"]` visible, heading text "Panel no disponible en móvil", heading is the focused element (accessible deep-link announcement), no `admin-nav-dashboard` present (US-9.2-S3) | `e2e/playwright-report/` | No horizontal overflow confirmed via `document.documentElement.scrollWidth <= clientWidth`; heading focus confirmed via `toBeFocused()` | N/A | **PASS** — 1/1 `mobile-chromium` |

## Categories (US-21.1, US-21.2)

| ID | Browser/Viewport | Route | Role/Fixtures | Stubs/Preconditions | Steps → Expected → Observed | Screenshot/trace | Overflow/KB/Touch | Provider | Result |
|---|---|---|---|---|---|---|---|---|---|
| BE-CAT-01 | Chromium desktop | `/administración` (Categories tab), `/` (public) | ADMIN (create/edit); anonymous (public render) | `AdminCategoriesPage.createCategory()` fills a deterministic local HTTPS fixture URL, not a real Cloudinary upload | Existing `admin-categories.spec.js` executed live in WU11: create/edit/delete a category with the required `image-url` field. After the 2026-08-23 follow-up, public Home `CategoryCard` renders the Lucide icon from `category.icon` (not `imageUrl`); category-filter click behavior is unchanged. Search filter rows show the same icon on one line. | `e2e/playwright-report/` | N/A | Cloudinary — admin URL fixture only; Home does not fetch the representative image | **PASS** — 4/4 `chromium` for admin image CRUD. Public icon render is covered by `CategoryCard.test.jsx` / `Home.test.jsx`. |

## Availability and Booking (US-23.1, US-23.2)

| ID | Browser/Viewport | Route | Role/Fixtures | Stubs/Preconditions | Steps → Expected → Observed | Screenshot/trace | Overflow/KB/Touch | Provider | Result |
|---|---|---|---|---|---|---|---|---|---|
| BE-AVAIL-01 | Chromium + Firefox desktop | `/lodgings/6` | USER (`e2e.testuser@…`) | `**/lodgings/6/availability**` routed to a delayed 200 with `occupiedRanges: []` | Load page → `role="status"` "Comprobando disponibilidad..." visible → after resolve, "Todas las fechas están disponibles." → date pickers enabled (US-23.1-S1, US-23.1-S2) | `e2e/playwright-report/` | N/A | N/A | **PASS** — `chromium`, `firefox` |
| BE-AVAIL-02 | Chromium + Firefox desktop | `/lodgings/6` | USER | availability routed to return one occupied range next month | Load page → no "all dates available" status → "Reservar" stays disabled with no range picked (US-23.1-S1 occupied-date semantics) | `e2e/playwright-report/` | N/A | N/A | **PASS** |
| BE-AVAIL-03 | Chromium + Firefox desktop | `/lodgings/6` | USER | availability routed to fail every request while routed, then unrouted before retry | Load page → `.availability-alert[role=alert]` with the fixed failure copy and "Reintentar" → unroute → click Reintentar → alert clears, "Todas las fechas están disponibles." (US-23.2-S1, US-23.2-S2) | `e2e/playwright-report/` | N/A | N/A | **PASS** |
| BE-AVAIL-04 | Chromium + Firefox desktop | `/booking/6` | USER | availability routed: dateless GET → available ranges `[]`; dated GET (preflight) → `available:false` in test 1, `available:true` + a `409` on `POST /reservations` in test 2 | Test 1: fill phone, pick a valid range → submit stays blocked until ready, then blocked by client preflight with inline `role=alert` "Las fechas seleccionadas ya no están disponibles..." and no navigation (US-23.2-S5). Test 2: preflight passes, `POST /reservations` returns 409 → inline alert shows the backend's message (final authority), `retryAvailability()` fires a recovery GET, no navigation | `e2e/playwright-report/` | N/A | N/A | **PASS** — 2/2 `chromium`, 2/2 `firefox` |

## Reviews (US-28.1, US-28.2)

| ID | Browser/Viewport | Route | Role/Fixtures | Stubs/Preconditions | Steps → Expected → Observed | Screenshot/trace | Overflow/KB/Touch | Provider | Result |
|---|---|---|---|---|---|---|---|---|---|
| BE-REV-01 | Chromium + Firefox desktop | `/lodgings/1` | anonymous | none | Load page → `.ratings-section` public display visible, `.review-form` absent, zero requests to `/ratings/lodging/*/eligibility` observed (US-28.2-S1) | `e2e/playwright-report/` | N/A | N/A | **PASS** |
| BE-REV-02 | Chromium + Firefox desktop | `/lodgings/2` (ineligible: fixture user has no reservation here) | USER | none — real eligibility check against the real DB | Load page → review-form heading visible, "Todavía no tenés una estadía confirmada y finalizada..." shown, no star-selector rendered (US-28.2-S2, and live proof of US-28.1-S6 wrong-lodging scoping) | `e2e/playwright-report/` | N/A | N/A | **PASS** |
| BE-REV-03 | Chromium + Firefox desktop | `/lodgings/1` (eligible: fixture reservation seeded above) | USER | eligibility-endpoint routed to fail once then unrouted (one sub-test); `POST /ratings` routed to `400` (one sub-test); real `POST /ratings` for the final sub-test | Sub-test A: eligibility fails → `.eligibility-alert[role=alert]` with Retry → unroute → click Retry → star-selector appears (US-28.2-S3). Sub-test B: eligible, pick 4★ + comment, real POST intercepted to fail → inline `.submit-error[role=alert]`, score/comment retained, not converted to apparent success (US-28.2-S4). Sub-test C: real successful 5★ submission (no interception) → form clears, submitted comment appears in the real public reviews list (round-trips through the real backend/DB) | `e2e/playwright-report/` | N/A | N/A | **PASS** — 3/3 sub-tests × `chromium`+`firefox` |

## WhatsApp (US-34.1, US-34.2, US-34.3)

| ID | Browser/Viewport | Route | Role/Fixtures | Stubs/Preconditions | Steps → Expected → Observed | Screenshot/trace | Overflow/KB/Touch | Provider | Result |
|---|---|---|---|---|---|---|---|---|---|
| BE-WA-01 | Chromium + Firefox desktop | `/` | anonymous and USER (two sub-tests) | `window.open` replaced with a fake-window double before click; real `wa.me` never contacted | Click "Contactar por WhatsApp" → fake `window.open('', '_blank')` invoked, `.opener` set to `null` before the URL assignment, URL assigned starts with `https://wa.me/` → `role="status"` feedback "Se abrió el acceso a WhatsApp; completá el envío allí." → text asserted to never match `/enviad|entregad|leíd/i` (US-34.1-S1, US-34.1-S2, US-34.2-S1) | `e2e/playwright-report/` | N/A | WhatsApp — handoff-request only, no delivery claim made or observable | **PASS** |
| BE-WA-02 | Chromium + Firefox desktop | `/` | anonymous | none | Button visible and enabled with no login (US-34.1-S1) | `e2e/playwright-report/` | N/A | N/A | **PASS** |
| BE-WA-03 | Mobile Chromium, 320×844 | `/` | anonymous | none | Button visible, ≥44×44 touch target, positioned in the lower/right region (`y > 300`), no horizontal overflow (US-34.1-S3) | `e2e/playwright-report/` | No horizontal overflow confirmed via `scrollWidth <= clientWidth`; touch target ≥44px confirmed | N/A | **PASS** |
| BE-WA-04 | Mobile Chromium, 390×844 | `/` | anonymous | none | Button visible, no horizontal overflow (US-34.1-S3) | `e2e/playwright-report/` | No horizontal overflow | N/A | **PASS** |
| BE-WA-05 | Chromium + Firefox desktop | `/` | anonymous | `window.open` stub returns `null` | Click button → `role="alert"` "No pudimos abrir WhatsApp. Habilitá las ventanas emergentes e intentá de nuevo." → button remains enabled for retry, no crash (US-34.3-S1) | `e2e/playwright-report/` | N/A | WhatsApp | **PASS** |
| BE-WA-06 | Chromium + Firefox desktop | `/` | anonymous | fake window's `location` setter throws | Click button → the fake window's `close()` is called, same actionable `role="alert"` failure copy shown, no success claimed (US-34.3-S2) | `e2e/playwright-report/` | N/A | WhatsApp | **PASS** |

**Non-verifiable in this environment (US-34.2-S2, invalid/missing number)**: `VITE_WHATSAPP_NUMBER` is inlined by Vite at **build** time; there is no runtime hook to swap it in a live browser page for an e2e test without a separate build. This scenario is fully covered instead by `frontend/src/components/WhatsAppButton/WhatsAppButton.test.jsx` (Vitest, `vi.stubEnv`, 5 invalid/missing-value cases) — see the traceability doc.

**Non-verifiable by design (US-34.3-S3, external delivery)**: TuHospedaje has no WhatsApp provider receipt, callback, or delivery/read evidence at any layer. This is the spec's own explicit boundary (design.md §6 "Provider observability boundary"), not a WU11 gap.

## Visual regression closeout

The Linux visual baselines under `e2e/tests/visual.spec.js-snapshots/` were regenerated through the repository's authorized GitHub Actions baseline workflow after manual PNG inspection. The legitimate changes correspond to the recommendation layout (WU2), category representative imagery (combined WU4/WU5 boundary), and WhatsApp control (WU10). A later reliability correction fulfills non-local image requests with a fixed local PNG during visual tests, removing an external-network race without masking local application layout. The final published PR #94 workflow completed successfully for Chromium, Firefox, and mobile Chromium before this history-only boundary replay. The replayed candidate retains the same application and snapshot content; its checks must run again after publication.
