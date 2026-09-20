# AdminDashboard section errors

## Scope

Show independent error and retry controls for dashboard statistics, recent lodgings, and recent reservations so one failed request does not hide successful sections.

## Tasks

- [x] Add independent request state and retry behavior for all three dashboard sections.
- [x] Add focused partial-failure and recovery tests.
- [x] Verify, review, and commit the work unit.

## Evidence

- Branch: `feat/admin-dashboard-section-errors`.
- Base: `69e5caa372add74bff42692ab3d36b9452b557cc`.
- Decision: independent per-section errors and retries.
- Issue: #269 (approved, `type:bug`).
- Verification: `npm test -- src/pages/Admin/AdminDashboard.test.jsx` — 14 passed; `git diff --check` — passed.
- Commit: `fix(frontend): show dashboard section load errors`.
