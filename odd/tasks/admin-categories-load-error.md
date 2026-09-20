# AdminCategories load error

## Scope

Show an explicit retryable error when the initial category catalog load fails, without changing mutation or refresh error handling.

## Tasks

- [x] Add an initial-load error state and retry to AdminCategories with failure and recovery tests.
- [x] Run focused verification, review the diff, and commit the work unit.

## Evidence

- Branch: `feat/admin-catalog-load-errors`
- Base: `4a4f3b3f9e1036307e1c154806d5f7fda612f177`
- Origin: frontend reliability audit.
- Issue: #259 (approved, `type:bug`).
- Verification: `npm test -- src/pages/Admin/AdminCategories.test.jsx` — 24 passed; `git diff --check` — passed.
- Commit: `fix(frontend): show admin category load error with retry`.
