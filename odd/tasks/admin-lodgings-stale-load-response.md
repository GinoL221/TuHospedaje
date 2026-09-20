# AdminLodgings stale load responses

## Scope

Prevent an older primary lodging-list request from replacing the state produced by a newer request.

## Tasks

- [x] Add a current-request guard to the primary AdminLodgings fetch and test late failure after a newer success.
- [x] Run focused tests and review the diff; commit the work unit.

## Evidence

- Branch: `fix/admin-lodgings-stale-load-response`
- Base: `baf25510ab270e830bd0dafb78c8e93345c9b273`
- Origin: non-blocking native-review advisory from PR #256.
- Issue: #257 (approved, `type:bug`).
- Verification: `npm test -- src/pages/Admin/AdminLodgings.test.jsx` — 19 passed; `git diff --check` — passed.
- Commit: `fix(frontend): ignore stale admin lodging list responses`.
