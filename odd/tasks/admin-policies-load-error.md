# AdminPolicies load error

- [x] Show retryable initial-load error instead of empty state.
- [x] Verify and commit.

## Evidence

- Issue: #265 (approved, `type:bug`).
- Verification: `npm test -- src/pages/Admin/AdminPolicies.test.jsx` — 16 passed.
- Diff check: passed.
- Commit: `fix(frontend): show admin policy load error with retry`.
