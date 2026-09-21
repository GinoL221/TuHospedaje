# Formatter and pre-commit enforcement tasks

## Scope

Add deterministic frontend formatting with Prettier, preserve the existing Gitleaks pre-commit protection, and enforce formatting in CI. Do not format the E2E package or alter unrelated working-tree changes.

## Tasks

- [x] Correct the PR #276 frontend CI formatting failure for `frontend/src/accountSharedAuditContract.test.jsx` and `frontend/src/services/lodgingService.js`.
  - Review: native review lineage `review-4cd7fa89f447b1c2` approved and acknowledged.
  - Signed commit: `ddb5165` (`style: format preserved frontend files`).
  - Validation: targeted Prettier 2/2, full format check, focused Vitest 2 files / 21 tests, and `git diff --check` passed.
  - Parent-owned: push and CI rerun remain parent-owned.

- [x] Add the frontend Prettier configuration, scripts, dependencies, and staged-file hook integration while preserving Gitleaks.
  - Evidence: `frontend/.prettierrc.json`, `frontend/.prettierignore`, `frontend/scripts/pre-commit.sh`, `frontend/package.json`, and `frontend/package-lock.json`.
  - Dependencies: Prettier 3.9.6, lint-staged 16.4.0 (Node 20 compatible), simple-git-hooks 2.14.0.
  - Validation: dependency/lockfile checks and `sh -n frontend/scripts/pre-commit.sh` passed; the initial source baseline had 136 Prettier violations.
- [x] Add frontend lint and format checks to CI.
  - Evidence: `.github/workflows/ci.yml` runs `npm run lint` and `npm run format:check` after `npm ci` and before coverage in the frontend job; both setup-node entries are pinned to Node `20.19`.
  - Validation: workflow diff contains only the two intended steps; read-only verification found no exact `node-version: '20'` entries and `git diff --check` passed. CI was not executed.
  - Review: natively reviewed and acknowledged under lineage `review-6700ccfd5e7b6940` across risk, resilience, readability, and reliability lenses for target `sha256:c2860dcfd6c9820d62bc2809243d531d42983bba1871789b2b22bac511d9bbee`.
  - Signed commit: `1f99356` (`ci: pin frontend jobs to Node 20.19`).
- [x] Correct the staged-file hook execution directory.
  - Evidence: `frontend/scripts/pre-commit.sh` enters `frontend` before `npm exec -- lint-staged`.
  - Validation: `sh -n frontend/scripts/pre-commit.sh` and `git diff --check` passed.
  - Review: natively reviewed and acknowledged under lineage `review-a3df00a2cfa74fa7` for target `sha256:eadc1e0f39772986ec1909f41d9a0bb45469961a178413ef9b489a429da97a15`.
- [x] Review the 14-file presentation/interactions formatting slice.
  - Scope: LodgingGallery, ProductCard, ReviewsSection, ShareModal, and WhatsAppButton.
  - Validation: targeted Prettier check and `git diff --check` passed.
  - Review: natively reviewed and acknowledged under lineage `review-ea95ab9d7c0c5abe` for target `sha256:a989b0211099aee1452fab07daff3aceffd50d7a89cfcb64549f6c21a2dfeb50`.
  - State: signed commit `be2a062` (`style: format lodging presentation components`).
- [x] Apply and verify the separate frontend formatting baseline while preserving local changes.
  - Evidence: 135 tracked frontend source files were formatted: 124 initial baseline files plus 11 omitted root-level files.
  - Compatibility adjustment: `frontend/src/publicShellContract.test.js` received one regex-only adjustment for Prettier's legal multiline CSS whitespace.
  - Preserved local files: the original local changes in `frontend/src/accountSharedAuditContract.test.jsx` and `frontend/src/services/lodgingService.js` were restored unchanged. The existing `frontend/src/components/ProductCard/ProductCard.css` change remains intact.
  - Final clean-worktree validation passed: ESLint, full `format:check`, coverage for 66/66 files and 621/621 tests, hook syntax/execution checks, and `git diff --check`; CI was not executed.
- [ ] Track the deferred WhatsApp open-redirect finding as a separate security task.
  - Scope boundary: this finding remains separate from the formatter work and needs separate security remediation and validation.
- [x] Prepare the clean formatter baseline and commit candidate while preserving unrelated local changes.
  - Native review inspect/start reached the provider, but no review authority was created because the complete 141-file candidate exceeded the provider reviewer context budget (`lens_context_budget_exceeded`).
  - The candidate must be split into smaller reviewable work units/commits before review.
- [x] Commit the remaining formatter work units after explicit commit authorization.
  - Signed work units completed so far: `12981cb` frontend shell, `848db66` shared components, `be2a062` lodging presentation, `96dd67a` admin shell/catalog, `44262f2` admin dashboard/features, `1f99356` CI Node pin, `1f71e26` admin lodging/policy pages, `9a7f528` admin reservations/users, `8377708` lodging form/table components, `37cb972` authentication guards, `90618ed` route loading components, `db5f35b` authentication context, `d42e2dc` authentication/search hooks, `d6c88ef` recommendation hooks, `c0ec9fd` rating eligibility tests, `adc981a` table data hooks, `a160bec` booking form components, `69d1e89` booking flow components, `eaf85f1` favorites/category pages, `145c9e9` home page, `a28dc4e` login/reservations pages, `b7c8a85` not-found/register pages, `6e45baf` unauthorized pages, `e865d5e` product detail page, `6011278` admin catalog services, `8d15624` API/auth services, `cc46f43` category/favorite services, `08b22ed` lodging/rating/refresh services, `2cacc2c` upload/date utilities, `0e20496` reservation utility maps, and `83724c3` public shell contract test fix.
  - Only the two protected local files remain modified; final validation passed.

## Constraints

- Keep `frontend/src/accountSharedAuditContract.test.jsx`, `frontend/src/services/lodgingService.js`, and the existing `ProductCard.css` working-tree changes intact unless the formatter changes are explicitly part of this feature.
- Do not read or modify any `.env` file.
- Use one formatter for frontend files; keep ESLint for semantic/style rules.
- Do not claim hook installation, CI execution, staging, or a commit without observed authorization and evidence.

## Evidence

- Investigation: `frontend/package.json` has ESLint and tests but no formatter or hook manager.
- Investigation: `.github/workflows/ci.yml` runs frontend coverage but no lint or format check.
- Investigation: local `.git/hooks/pre-commit` runs Gitleaks and must be preserved or chained.

## Latest evidence

- The versioned hook now enters `frontend` before running `npm exec -- lint-staged`; `sh -n frontend/scripts/pre-commit.sh` and `git diff --check` passed. The correction was natively reviewed and acknowledged under lineage `review-a3df00a2cfa74fa7` for target `sha256:eadc1e0f39772986ec1909f41d9a0bb45469961a178413ef9b489a429da97a15`.
- Both CI `setup-node` entries are pinned to Node `20.19`; read-only verification found no exact `node-version: '20'` entries and `git diff --check` passed. CI execution has not been claimed. The change was reviewed and acknowledged under lineage `review-6700ccfd5e7b6940` across all four lenses and committed with signature as `1f99356`.
- The 14-file presentation/interactions formatting slice covering LodgingGallery, ProductCard, ReviewsSection, ShareModal, and WhatsAppButton was reviewed and acknowledged under lineage `review-ea95ab9d7c0c5abe` for target `sha256:a989b0211099aee1452fab07daff3aceffd50d7a89cfcb64549f6c21a2dfeb50`. Targeted Prettier check and `git diff --check` passed. The signed commit is `be2a062`.
- The 5-file admin shell/catalog formatting slice (`Admin.css`, `Admin.jsx`, `Admin.test.jsx`, `AdminCategories.jsx`, and `AdminCategories.test.jsx`) was reviewed and acknowledged under lineage `review-6640974d5b899b8c` for target `sha256:941d20a0e8932643ca8162930bc1e0f493a1457ac3892eb446f8ef6bc44ea82d`. Targeted Prettier check and `git diff --check` passed. The signed commit is `96dd67a`.
- A 16-file admin candidate exceeded the native reviewer context budget and created no authority; it was split into smaller candidates. The 4-file dashboard/features slice (`AdminDashboard.jsx`, `AdminDashboard.test.jsx`, `AdminFeatures.jsx`, and `AdminFeatures.test.jsx`) was reviewed and acknowledged under lineage `review-29cc803d4be1070b0` for target `sha256:03faad26e00e1b53b3b04f3c48e2e1dda1ec7ae0827f66af3aeab7a7e7a43a76`. Targeted Prettier check and `git diff --check` passed. The signed commit is `44262f2`.
- The 4-file admin lodging/policy slice (`AdminLodgings.test.jsx`, `AdminPolicies.jsx`, `AdminPolicies.test.jsx`, and `AdminReservations.jsx`) was reviewed and acknowledged under lineage `review-919c28e7ecc85aaf` for target `sha256:ef2db245cff384a922832846e1050f7b27287a3c511989c5d7a8bf1399b603c3`. Targeted Prettier check and `git diff --check` passed; the signed commit is `1f71e26`.
- The 3-file admin reservations/users slice (`AdminReservations.test.jsx`, `AdminUsers.jsx`, and `AdminUsers.test.jsx`) was reviewed and acknowledged under lineage `review-6e3b7dd1474e0934` for target `sha256:82a6faec5e0b098e2e00156965b75485ab8709bfb9f231d621def028200896bc`. Targeted Prettier check and `git diff --check` passed; the signed commit is `9a7f528`.
- The 5-file lodging form/table slice (`LodgingFormModal.jsx`, `LodgingFormModal.test.jsx`, `LodgingsTable.jsx`, `Pagination.jsx`, and `Pagination.test.jsx`) was reviewed and acknowledged under lineage `review-7eb869fe25b4cc04` for target `sha256:35d86baa403a28c37e83099b9a144dbae2ef59c74cea30ad499a8844839d9314`. Targeted Prettier check and `git diff --check` passed; the signed commit is `8377708`.
- The 4-file authentication guard slice (`RequireAdmin.jsx`, `RequireAdmin.test.jsx`, `RequireAuth.jsx`, and `RequireAuth.test.jsx`) was reviewed and acknowledged under lineage `review-f85daac0536bf0ac` for target `sha256:254fdc7e9deb2bfb2bd71a1f3861388c678be3d5d8feecbcdc401f0548599975`. Targeted Prettier check and `git diff --check` passed; the signed commit is `37cb972`.
- The 5-file route/loading slice (`RouteChunkErrorBoundary.jsx`, `RouteChunkErrorBoundary.test.jsx`, `RouteLoadingFallback.jsx`, `RouteLoadingFallback.test.jsx`, and `SortableTh.jsx`) was reviewed and acknowledged under lineage `review-6e53cbf7cbbe12dc` for target `sha256:251ba8c270c705743ef2db646221ee71360740f731cde677a3bd3f7bd5746811`. Targeted Prettier check and `git diff --check` passed; the signed commit is `90618ed`.
- The 3-file authentication context slice (`AuthContext.jsx`, `AuthContext.test.jsx`, and `AuthContextCsrfRace.test.jsx`) was reviewed and acknowledged under lineage `review-fb770b6ef8ad073d` for target `sha256:b637fd3be5848b9b330b5b05e1ae72865856a8c9815e5d98791f40e5d5b5503b`. Targeted Prettier check and `git diff --check` passed; the signed commit is `db5f35b`.
- The 5-file authentication/search hooks slice (`useAuth.js`, `useAvailability.test.js`, `useCityAutocomplete.js`, `useConfirmCancel.js`, and `useConfirmCancel.test.js`) was reviewed and acknowledged under lineage `review-62e44ae6d796423e` for target `sha256:f5973140866cd9be4e1f69f6eb6564c5c93c401bfc894732b6907875d24b7627`. Targeted Prettier check and `git diff --check` passed; the signed commit is `d42e2dc`.
- The 3-file recommendation hooks slice (`useHomeRecommendations.js`, `useHomeRecommendations.test.js`, and `useRatingEligibility.js`) was reviewed and acknowledged under lineage `review-f19a9551309b9ca0` for target `sha256:4c09b4de624cd9e6a52d0636caa2dcfe41cc4dbe18dc240d00e231b656f678e4`. Targeted Prettier check and `git diff --check` passed; the signed commit is `d6c88ef`.
- The 1-file rating eligibility test slice (`useRatingEligibility.test.js`) was reviewed and acknowledged under lineage `review-2c398cd6b90509e6` for target `sha256:504401656eacf86ca85cb19333a19f17e743308eef681673ed5242e6530a31cd`. Targeted Prettier check and `git diff --check` passed; the signed commit is `c0ec9fd`.
- The 2-file table data hooks slice (`useTableData.js` and `useTableData.test.js`) was reviewed and acknowledged under lineage `review-53ba47e6109e5ff4` for target `sha256:b97ca1da0840d9f0c4b2e50c02f3e6905de1f3c17de020ab1f11a20605b051c5`. Targeted Prettier check and `git diff --check` passed; the signed commit is `adc981a`.
- The 5-file booking form slice (`BookingConfirmation.css`, `BookingConfirmation.jsx`, `BookingConfirmation.test.jsx`, `BookingDateFields.jsx`, and `BookingPage.css`) was reviewed and acknowledged under lineage `review-907a37f9338f73de` for target `sha256:4c0f5c014ba7dce09cb42ea0989dba628fcf7e0a6862aaee3872a51e0576eef0`. Targeted Prettier check and `git diff --check` passed; the signed commit is `a160bec`.
- The 4-file booking flow slice (`BookingPage.jsx`, `BookingPage.test.jsx`, `BookingSummary.jsx`, and `GuestDetails.jsx`) was reviewed and acknowledged under lineage `review-7c4aabe36cef4fde` for target `sha256:000b9308440bfda7d7529e7d297facd0191aced7c55ed27fe5ec45e067098536`. Targeted Prettier check and `git diff --check` passed; the signed commit is `69d1e89`.
- The 4-file favorites/category slice (`FavoritesPage.jsx`, `FavoritesPage.test.jsx`, `CategoryCard.jsx`, and `CategoryCard.test.jsx`) was reviewed and acknowledged under lineage `review-013ee3f98c87d2b1` for target `sha256:45177482d991d7792260596b967660bdb56d946fb71c6ab86ec6816fbac75f25`. Targeted Prettier check and `git diff --check` passed; the signed commit is `eaf85f1`.
- The 3-file home page slice (`Home.css`, `Home.jsx`, and `Home.test.jsx`) was reviewed and acknowledged under lineage `review-936422738ddf82fe` for target `sha256:9eb83203c7d2b28dab7c84b3c432739de929713ddb0a94d210957ec4c0b43c77`. Targeted Prettier check and `git diff --check` passed; the signed commit is `145c9e9`.
- The 5-file login/reservations slice (`LoginPage.jsx`, `LoginPage.test.jsx`, `MyReservationsPage.css`, `MyReservationsPage.jsx`, and `MyReservationsPage.test.jsx`) was reviewed and acknowledged under lineage `review-e6cccd011f991f9f` for target `sha256:7a9995f9b5a1cd465a7cfdd6580602d7c979eb9168ca26c5cd29c1bc5c3b068e`. Targeted Prettier check and `git diff --check` passed; the signed commit is `a28dc4e`.
- The 3-file not-found/register slice (`NotFound.jsx`, `NotFound.test.jsx`, and `RegisterPage.jsx`) was reviewed and acknowledged under lineage `review-8567c9d552dd1864` for target `sha256:00c3bf43e65638fa62425bafc0a21acbc7d6d8fbee4780a0c4d28bca58f36065`. Targeted Prettier check and `git diff --check` passed; the signed commit is `b7c8a85`.
- The 3-file unauthorized/register-test slice (`RegisterPage.test.jsx`, `Unauthorized.jsx`, and `Unauthorized.test.jsx`) was reviewed and acknowledged under lineage `review-43862be2317f6b8c` for target `sha256:3a69ef616526ee5748f593ed56a7aee509d54686390a794389de13977e554c8c`. Targeted Prettier check and `git diff --check` passed; the signed commit is `6e45baf`.
- The 3-file product detail slice (`ProductDetail.css`, `ProductDetail.jsx`, and `ProductDetail.test.jsx`) was reviewed and acknowledged under lineage `review-ca8c63531c505f70` for target `sha256:0d592b3c6fc8655f205a6e03a93fc092e3efe099c80acd2ae05bc9fa0833fa0c`. Targeted Prettier check and `git diff --check` passed; the signed commit is `e865d5e`.
- The 3-file admin catalog/API-CSRF slice (`adminCatalogService.js`, `adminCatalogService.test.js`, and `api.csrf.test.js`) was reviewed and acknowledged under lineage `review-36f2cb2f164fbe89` for target `sha256:f0071f561947f2dbc66b7734bd36d0c8fdca32cc4fe09b823ac1d59f641714df`. Targeted Prettier check and `git diff --check` passed; the signed commit is `6011278`.
- The 4-file API/auth slice (`api.js`, `api.test.js`, `authService.js`, and `authService.test.js`) was reviewed and acknowledged under lineage `review-f64e3a0d41bfc2c8` for target `sha256:ba0b4d70414b8242291549c3249ed7bf58bdc9d3d09a9e91eb3020f5d7c39fd7`. Targeted Prettier check and `git diff --check` passed; the signed commit is `8d15624`.
- The 4-file category/favorite service slice (`categoryService.js`, `categoryService.test.js`, `favoriteService.js`, and `favoriteService.test.js`) was reviewed and acknowledged under lineage `review-e716aa5981afd4e9` for target `sha256:637822005e704afb3222d93a0bea2291562e78662a6645893528a2dfe86e8d14`. Targeted Prettier check and `git diff --check` passed; the signed commit is `cc46f43`.
- The 5-file lodging/rating/refresh slice (`lodgingService.test.js`, `ratingService.js`, `ratingService.test.js`, `refreshCoordinator.js`, and `refreshCoordinator.test.js`) was reviewed and acknowledged under lineage `review-0637b3465cfab8d9` for target `sha256:5d5fb4183cef0452d7bb808ba6e7927979ad66f6b5232fda39cdcb4242b3b03c`. Targeted Prettier check and `git diff --check` passed; the signed commit is `08b22ed`.
- The 5-file upload/date utility slice (`uploadService.js`, `uploadService.test.js`, `test-utils.test.jsx`, `dateRange.js`, and `dateRange.test.js`) was reviewed and acknowledged under lineage `review-bb1900c25e161b1a` for target `sha256:60882a21092cdb393ceae5380f90f67da4651a9bea22b0611b30f65541198f5f`. Targeted Prettier check and `git diff --check` passed on the corrected worktree rerun; the signed commit is `2cacc2c`.
- The 2-file reservation utility slice (`iconMap.js` and `reservationPresentation.js`) was reviewed and acknowledged under lineage `review-391482ce43356288` for target `sha256:16846fc3fab95cf7b64f17661c1d09365e32d70950c04bc91b90ef328e57a99c`. Targeted Prettier check and `git diff --check` passed; the signed commit is `0e20496`.
- The focused `publicShellContract.test.js` regex fix was reviewed and acknowledged under lineage `review-9007ec6cb67155fc` for target `sha256:fcaaeee780957cc5e37d77e5f4fc987952beec6688af87042d1fef9ada43f0d4`. Targeted Prettier check, the 4-test contract suite, and `git diff --check` passed; the signed commit is `83724c3`.
- Final clean-worktree validation after `83724c3` and `c922253`: `npm run lint`, `npm run format:check`, and `npm run coverage` passed (66 files, 621 tests); hook syntax/execution and `git diff --check` passed. Only `accountSharedAuditContract.test.jsx` and `lodgingService.js` remain modified.
