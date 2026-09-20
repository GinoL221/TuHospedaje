# AdminUsers load error

## Scope

Show a retryable initial-load error instead of the successful empty-user state.

## Tasks

- [x] Add initial-load error and retry behavior with focused tests.
- [x] Verify, review, and commit the work unit.

## Evidence

- Branch: `feat/admin-users-load-error`.
- Base: `7b2f29e4ea2715971b888f4dbd5c57473ede66b0`.
- Issue: #267 (approved, `type:bug`).
- Verification: `npm test -- src/pages/Admin/AdminUsers.test.jsx` — 10 passed; `git diff --check` — passed.
- Commit: `fix(frontend): show admin user load error with retry`.
