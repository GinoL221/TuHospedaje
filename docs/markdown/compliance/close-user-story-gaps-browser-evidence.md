# Browser and CI Evidence — Phase 2A Reconciliation

**Scope:** current documentation evidence for commit `c6f50499b6f7d8fde2567cfae80eab35cc1dad06` only.

## Evidence boundary

The current revision-bound evidence is GitHub Actions run **`35661433657`** at commit **`c6f50499b6f7d8fde2567cfae80eab35cc1dad06`**, with conclusion **success**. Parent verification observed these job-level results: backend 651 tests, zero failures, `BUILD SUCCESS`; frontend 66 files/621 tests, plus lint and coverage; desktop Chromium 58 passed; desktop Firefox 58 passed; and mobile Chromium 31 passed. Two mobile booking scenarios failed once and passed on the CI retry. Desktop WhatsApp contract tests passed in Chromium and Firefox, and mobile-shell WhatsApp placement tests passed. These are job-level results, not proof of every acceptance criterion or external-provider outcome.

Earlier PR #94, PR #233, and older-candidate browser records are historical context. They are not current-candidate evidence and are not used to claim a current result.

| Evidence ID | Candidate and scope | Observed/reference evidence | Result boundary |
| --- | --- | --- | --- |
| BE-CI-01 | All documentation claims tied to the current candidate | GitHub Actions run `35661433657` concluded success at `c6f50499b6f7d8fde2567cfae80eab35cc1dad06`: backend 651 tests/zero failures/`BUILD SUCCESS`; frontend 66 files/621 tests with lint and coverage; Chromium 58 passed; Firefox 58 passed; mobile Chromium 31 passed. | Job-level evidence only. Two mobile booking scenarios required one CI retry before passing; no trace, screenshot, or criterion-by-criterion assertion is claimed here. |
| BE-WA-CI-01 | US #34 WhatsApp handoff control | In run `35661433657`, desktop WhatsApp contract tests passed in Chromium and Firefox; mobile-shell WhatsApp placement tests passed. | This replaces stale “pending CI” and older-candidate claims with observed current CI results. Any URL/link result is handoff-only; it does not establish external WhatsApp execution, delivery, read status, provider-side error handling, or a success notification after sending. |
| BE-FAV-BOUNDARY-01 | US #24–#25 favorites | Current mobile tests stub the favorites API | Browser behavior under stubs is not real-backend favorites E2E evidence. Persistence, cross-session state, and real-time synchronization remain partial/non-verifiable. |
| BE-IMAGE-BOUNDARY-01 | US #3, #6, #21 lodging/category images | No current upload or canonical-image artifact was inspected | Upload completion, storage/provider publication, canonical image availability, and representative-image rendering must remain partial/non-verifiable. |
| BE-SHARE-BOUNDARY-01 | US #27 sharing and US #34 WhatsApp | A browser can hand off to an external URL only | External social/WhatsApp provider execution, redirect completion, publication, delivery, and read receipts are outside local browser evidence. |
| BE-EMAIL-BOUNDARY-01 | US #35 reservation-confirmation email | No Mailtrap inbox or equivalent recipient/provider receipt was inspected | Reservation email recipient delivery, timing, inbox rendering, and content receipt are not claimed. |

## Traceability use

Use `BE-CI-01` as the candidate identity reference and the boundary rows above when reading the official US #1–#35 matrix in `close-user-story-gaps-traceability.md`. The matrix retains US #19 as explicitly optional and non-evaluated, and keeps US #36–#43 outside the official Sprint scope.

## Handoff-only and no-delivery boundary

A `wa.me` or social-sharing URL is a handoff to a provider, not proof that the provider executed the action. This documentation change performs no provider call and authorizes no commit, push, pull request, or release.
