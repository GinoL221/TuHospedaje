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
- [ ] Deliver category failure/retry UI and tests as a separate verified commit at or below 400 changed lines.
- [ ] Run complete frontend checks and read-only review of both new commit candidates; record exact evidence and slice boundaries.

## Verification
- Test first candidate on its own branch head with focused Home/hook tests, lint, build, diff check.
- Test both together using the full frontend suite, lint, build, and changed-file diagnostics.
- Treat previous native review `review-b8597dee88fef688` as evidence for the old commit only, not the new commit identities.
- Record limitations; browser/provider/CI are not inferred from local tests.

## Evidence
- RED: `npm --prefix frontend test -- src/pages/Home/Home.test.jsx src/hooks/useHomeSearchResults.test.js` failed as expected before implementation: 2 new Home search-failure tests could not find the alert (35 passed, 2 failed).
- GREEN: the same focused command passed after implementation (41 passed, 0 failed).
- `git diff --check` passed before parent verification and commit.
- Independent verification passed: focused 41/41, full frontend 638/638 across 66 files, ESLint, Vite build (2,153 modules), and diff check. Category behavior remains at baseline; search failure uses one alert live region.
- Search slice is under the 400-line budget; category failure/retry work is not included. Commit identity pending.
