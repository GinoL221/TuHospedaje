# AdminCategories stale load responses

## Scope

Prevent superseded category load and retry requests from updating the current category state.

## Tasks

- [x] Add a request-sequence guard and a late-response race test.
- [x] Run focused verification and commit the work unit.

## Evidence

- Branch: `fix/admin-categories-stale-load-response`
- Base: `5aceddd9057fb37cae2d9be23b10ed761df0c76b`
- Origin: non-blocking native-review advisory from PR #260.
- Issue: #261 (approved, `type:bug`).
- Verification: `npm test -- src/pages/Admin/AdminCategories.test.jsx` — 25 passed; `git diff --check` — passed.
- Commit: `fix(frontend): ignore stale admin category responses`.
