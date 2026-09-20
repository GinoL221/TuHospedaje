# AuthContext service boundary

## Scope

Move authentication endpoint calls and CSRF sequencing out of `AuthContext` into `frontend/src/services/authService.js` without changing provider behavior.

## Tasks

- [x] Add authService functions for session bootstrap, login, register, and logout.
- [x] Update AuthContext and tests to consume the service boundary.
- [x] Verify, review, and commit the work unit.

## Constraints

- Preserve the current CSRF-before-publish sequencing.
- Preserve register's clear retry-via-login error when CSRF bootstrap fails.
- Preserve generation guards and logout error behavior.
- Do not reorganize the broader frontend architecture.

## Evidence

- Branch: `refactor/auth-context-service`.
- Base: `bb0dcfb9acab36868b4034989a357581a7112254`.
- Issue: #271 (approved, `type:chore`).
- Verification: AuthContext/service suite — 18 passed; `git diff --check` — passed.
- Boundary check: AuthContext has no direct `services/api` import.
- Commit: `refactor(frontend): extract auth context service boundary`.
