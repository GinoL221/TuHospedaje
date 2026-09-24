# Split Home search and category retries

## Goal
Rebuild the verified Home request-failure change as two independently reviewable units: complete search failure/retry UX, then complete category failure/retry UX.

## Boundaries
- Start: `origin/main` at `8206cf4a7d8b4a5b733c45d98fa6ae163885d797`.
- Source of truth for unchanged behavior: `fix/home-search-category-error-retry` at `fea5191030cf7e01dbd890cde180ee0e66cd3554`. Keep that branch and worktree intact.
- Allowed source files: `frontend/src/hooks/useHomeSearchResults.js`, its test, `frontend/src/pages/Home/Home.jsx`, its test.
- Do not change service APIs, recommendations, favorites, form validation, routes, visual styling, or other worktrees. No push, issue creation, PR, merge, or cleanup without separate approval.

## Tasks
- [x] Deliver search failure/retry UI, hook, and tests as one verified commit at or below 400 changed lines.
- [x] Deliver category failure/retry UI and tests as a separate verified commit at or below 400 changed lines.
- [x] Run complete frontend checks and native review of both new commit candidates; record exact evidence and slice boundaries.
- [x] Remove the assertion-free category unmount test, state the actual coverage boundary, and reverify/review the correction.
- [x] Replace the category nested ternary with explicit state selection; preserve all three rendered states, then verify and review a separate commit in the category slice.

## Verification
- Test first candidate on its own branch head with focused Home/hook tests, lint, build, diff check.
- Test both together using the full frontend suite, lint, build, and changed-file diagnostics.
- Native approval `review-eaa19f60d451faaa` applies only to first commit `5475eb4affecae9e42986a5133063ac63dd1509e`. The second slice and later corrections have separate commit-bound approvals recorded below.
- Record limitations; browser/provider/CI are not inferred from local tests.

## Evidence
- RED: `npm --prefix frontend test -- src/pages/Home/Home.test.jsx src/hooks/useHomeSearchResults.test.js` failed as expected before implementation: 2 new Home search-failure tests could not find the alert (35 passed, 2 failed).
- GREEN: the same focused command passed after implementation (41 passed, 0 failed).
- `git diff --check` passed before parent verification and commit.
- Independent verification passed: focused 41/41, full frontend 638/638 across 66 files, ESLint, Vite build (2,153 modules), and diff check. Category behavior remains at baseline; search failure uses one alert live region.
- First slice commit: `5475eb4affecae9e42986a5133063ac63dd1509e` (`fix: show Home search failures with retry`), signed and approved by native review `review-eaa19f60d451faaa`.
- Second slice RED: `npm --prefix frontend test -- src/pages/Home/Home.test.jsx src/hooks/useHomeSearchResults.test.js` failed as expected before the category implementation (43 passed, 2 failed: category failure alerts absent).
- Second slice GREEN: the same focused command passed after implementation (45 passed, 0 failed). It covers category failure versus valid empty response, independent retry recovery, and stale retry completion. A separate unmount test ran without assertions and did not prove the cleanup guard; follow-up task removes that misleading claim.
- Independent verification passed: focused 45/45, full frontend 642/642 across 66 files, ESLint, Vite build (2,153 modules), and diff check. Home source/test files match the preserved original final revision byte-for-byte; the second diff does not change search or recommendations.
- Second slice signed commit: `be48aacae0e55977ec5980fac7c8898db9b652d0` (`fix: show Home category loading failures with retry`), 161 changed lines against `5475eb4affecae9e42986a5133063ac63dd1509e`; approved and acknowledged native review `review-17e0873be4c438ed`.
- First slice: 378 changed lines against `origin/main`, approved and acknowledged native review `review-eaa19f60d451faaa`. Both reviews bound only their committed slices.
- Changed-file LSP probe reported zero diagnostics but all four files inconclusive (push-only server cannot confirm clean); full ESLint, build, and tests passed at each candidate head. CI, browser and provider checks have not run.
- Original completed branch/worktree remains unchanged; no issue, push, PR, merge or cleanup performed.
- Follow-up investigation: the mounted stale-retry test asserts that older completions do not replace newer UI. The category cleanup invalidates request IDs on unmount by code inspection; RTL/JSDOM does not expose a reliable direct post-unmount state-update assertion here without testing React internals. The assertion-free unmount test was removed.
- Reverification after fixing a blank-line Prettier failure: focused 44/44, full frontend 641/641 across 66 files, ESLint, format check, Vite build (2,153 modules), coverage and `git diff --check` passed. Coverage: statements 93.42% (threshold 85%), branches 88.71% (80%), functions 89.44% (74%), lines 95.08% (87%). No production change.
- Signed follow-up work-unit commit `4c928d363c4694b89427dc2f57b6d7390911e09d` (25 diff lines) is approved and acknowledged in native review `review-e3e50076ff1372dc`. At passive evidence commit `2142d17b387b21d60245f6731da1b02d617c8a96`, the intermediate category PR slice totaled 160 diff lines against `5475eb4affecae9e42986a5133063ac63dd1509e`; later corrections increased that count.
- Before this follow-up, all-server diagnostics showed two clean files, two with auxiliary findings, none inconclusive. The candidate-caused category nested-ternary warning was addressed by choosing `categoryContent` through explicit error/empty/list branches; a fresh Home.jsx probe reports only an informational Spanish typo suggestion, with no nested-ternary warning.
- Independent post-format verification: focused 44/44 and full frontend 641/641 across 66 files; ESLint, Prettier format check, Vite build (2,153 modules), coverage thresholds and diff check passed. Coverage: statements 93.44% (85% required), branches 88.71% (80%), functions 89.44% (74%), lines 95.10% (87%).
- Signed render-cleanup work-unit commit `9d9dc74e84f5b72448f7d17d5a172a74a08adc0a` changed 61 lines; native review `review-5c09e498b52359be` approved and acknowledged. The category PR slice is 201 changed lines against `5475eb4affecae9e42986a5133063ac63dd1509e` before this final evidence note. No issue, push or PR created.
