# Last enabled administrator invariant

## Goal

Guarantee at the backend transaction boundary that every committed user-management state retains at least one enabled `ADMIN`.

## Scope

Protect only transitions that can remove an enabled administrator:

- `ADMIN + enabled=true -> USER`
- `ADMIN + enabled=true -> enabled=false`

This work unit does not introduce a master-admin identity, change frontend behavior, make Admin mobile-responsive, or alter role/session policy beyond the existing disable-session revocation contract.

## Design

1. Add a one-row `admin_invariant_lock` table and acquire its fixed row with `PESSIMISTIC_WRITE` before every destructive administrator transition.
2. After acquiring the stable mutex, lock all currently enabled administrators in ascending ID order and reject the change when the current set contains only the target.
3. Add a composite `users(role, enabled, id)` index in the same Flyway migration so the current-state lookup uses the relevant range.
4. Keep promotions, re-enables, and true no-op updates outside both lock paths.
5. Preserve refresh-session revocation only for a real `enabled=true -> false` transition. A rejected change or no-op must not revoke sessions.
6. Return `409 Conflict` with a stable `last_enabled_admin` code through `GlobalExceptionHandler`.

The initial design locked only the mutable enabled-admin set. A deterministic MariaDB test reproduced deadlock 1213 and a zero-admin final state for two simultaneous disables. The user authorized this bounded redesign; the stable coordination row now serializes destructive transitions before the mutable set is read.

## Tasks

- [x] Add failing unit tests for last-admin rejection, allowed multi-admin transitions, promotions/re-enables, and no-op behavior.
- [x] Add failing HTTP integration tests for the stable `409` response and unchanged persisted state.
- [x] Add failing MariaDB concurrency tests proving simultaneous demotions and disables cannot leave zero enabled admins.
- [x] Add the stable coordination row, Flyway index, pessimistic repository queries, domain exception, handler/messages, OpenAPI response, and service guard.
- [x] Preserve valid disable-session revocation and prove rejected/no-op operations do not revoke sessions or emit disable events.
- [x] Run focused unit and integration tests, the backend verification suite, migration checks, and static diagnostics. Independent verification passed 664 tests, JaCoCo coverage, and `git diff --check`; LSP reported no findings but all 12 files were inconclusive because the server cannot confirm a clean re-check.
- [x] Commit the coherent implementation and tests as one Conventional Commit work unit: `6494757aed560e81d964ec91790dd0ba79389c44` (`fix: preserve at least one enabled admin`). The user-authorized history rewrite successfully signed the commit with GPG.
- [x] Run the native review gate for the exact commit and record final evidence without pushing or cleaning worktrees. Review `review-95093ba400a47900` approved and acknowledged target `sha256:45a183e55216d69d66f7d79f71d7068e71fcb3064831a40cc587c505b428f20b`.

## Acceptance criteria

- Demoting or disabling the only enabled administrator returns `409` with code `last_enabled_admin` and changes nothing.
- With two or more enabled administrators, one may be demoted or disabled and exactly one may remain.
- Concurrent destructive transitions serialize so one succeeds and one is rejected; zero enabled administrators is never committed.
- `ADMIN -> ADMIN`, `USER -> USER`, `enabled -> same value`, promotion, and re-enable remain successful no-ops/additive transitions without collective locking or unnecessary saves.
- Refresh sessions are revoked exactly once only after a real allowed disable transition.
- The new migration applies on MariaDB and Hibernate validation remains clean.
- Existing role, disablement, authentication, and session tests continue to pass.
- The final diff stays inside backend invariant/test/migration surfaces plus these two ODD artifacts.

## Verification plan

- Focused unit test: `./mvnw -Dtest=UserServiceImplTest test`.
- Focused HTTP/session tests: `./mvnw -Dtest=UserControllerIntegrationTest,UserControllerAdminDisableIntegrationTest test`.
- Focused concurrency test: `./mvnw -Dtest=UserAdminInvariantConcurrencyIntegrationTest test`.
- Backend suite: `./mvnw -B verify`.
- `git diff --check`, LSP diagnostics, exact-scope inspection, and native review.

## Evidence

- Focused final verification: 31 tests passed.
- Independent backend verification: 664 tests passed with zero failures/errors/skips; JaCoCo passed.
- `git diff --check`: passed.
- LSP: no findings returned; all 12 queried Java files remained inconclusive because the server cannot affirm a clean re-check.
- Native review: approved and acknowledgement burned for `review-95093ba400a47900`.
- Push/merge/cleanup: not performed.

## Delivery

- Branch: `fix/last-admin-invariant-review`, based directly on `c6f50499`.
- Review workload: approximately 629 changed lines, dominated by concurrency/unit coverage. The user authorized a size exception because separating the concurrency behavior from its proof would make the work unit less reviewable.
- Local work-unit commits only. Do not push, open a pull request, merge, or remove worktrees without explicit user authorization.
