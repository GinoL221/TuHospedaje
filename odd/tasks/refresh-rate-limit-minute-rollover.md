# Refresh rate-limit minute rollover

## Goal
Make the valid-credential refresh IP-rate-limit integration test tolerate a real fixed-window minute rollover without weakening its full HTTP-chain assertion.

## Scope

Test code:
- `backend/src/test/java/com/tuhospedaje/auth/RefreshRateLimitIntegrationTest.java`

Task evidence:
- `odd/tasks/refresh-rate-limit-minute-rollover.md`
- `odd/refresh-rate-limit-minute-rollover/tasks`

Do not change production rate-limit behavior, clocks, session/JWT configuration, refresh-family semantics, workflow files, environment files, or unrelated tests.

## Tasks

- [x] Add bounded attempt headroom to the valid-credential IP-ceiling test and preserve fresh-family isolation.
- [x] Run focused/full backend verification, record evidence, and complete the native review gate.

## Acceptance criteria

- The valid-credential IP-ceiling test is not limited to exactly `IP_LIMIT + 1` attempts against the real system clock.
- Each attempt still registers and logs in a distinct user/family on the same IP.
- The test still requires an eventual HTTP `429`, `Retry-After`, and the expected JSON error/status body.
- Attempt count remains bounded and follows the existing integration-test precedent.
- Comments describe rollover tolerance accurately and no longer claim exactly six attempts.
- No production file changes.
- No environment file or secret is read or changed.

## Verification plan

- Observed RED: CI run `35925301333`, backend job `107398841839`, failed only `RefreshRateLimitIntegrationTest.ipCeilingExceededWithValidCredentialsAlsoReturns429WithRetryAfter` because no `429` appeared across six attempts.
- Focused class: `./mvnw -B -Dtest=RefreshRateLimitIntegrationTest test` from `backend/`.
- Full backend: `./mvnw -B verify` from `backend/`.
- `git diff --check`.
- LSP diagnostics for the changed Java test.

## Evidence

### Bounded rollover tolerance

- RED: CI run `35925301333`, backend job `107398841839`, ran 664 tests and failed only the valid-credential IP-ceiling test because no `429` appeared in exactly six attempts.
- Root cause: six requests can split across a real epoch-minute boundary, resetting the fixed-window bucket before any bucket receives its sixth hit.
- The test now allows `(IP_LIMIT + 1) * 2 = 12` attempts. Across at most two windows, pigeonhole reasoning guarantees one bucket receives at least six attempts.
- Each attempt keeps IP `10.2.1.9` and creates a distinct valid user/family.
- Focused class passed 8/8 tests with zero failures, errors, or skips after the user-authorized cleanup of a corrupted generated `backend/target` class.
- Independent verification repeated 8/8 focused tests, confirmed a test-only 10-addition/3-deletion diff, and passed focused `git diff --check`.
- Work-unit commit: `0bb4affb5310b6b5a40e621caf4735b68b33e4ab` (`test: tolerate refresh rate-limit rollover`). GPG failed with `Vida máxima`; the user explicitly authorized this commit without a signature.
- The first full-suite attempt ran 664 tests but ended with four configuration errors because `JWT_SECRET` was absent; no test assertion failed.
- User-authorized verification with a generated process-only `JWT_SECRET` passed 664/664 tests, generated the JaCoCo report for 86 classes, and passed all coverage checks.
- Full `git diff --check HEAD` passed and the working tree was clean.
- LSP reported no diagnostics but was inconclusive because the Java server is silent on clean re-checks.
- Verification evidence commit: `2ebaa04aad4387f1c271ee16bb568cb4b26d4e6b` (`docs: record refresh rollover verification`). GPG again failed with `Vida máxima`; the user explicitly authorized this commit without a signature.
- Native review `review-59a14d739f4f324e` approved and acknowledged target `sha256:4961f1c926f410b968c1c66888d68c19a7155f1187532a14d3870d95b72ec33b`.
- Review advisories `R2-bounded-window-assumption`, `R3-minute-rollover-bound`, and `R4-minute-rollover-third-window` are informational and all describe the documented three-or-more-window residual risk; no correction was opened.
- Residual risk: the bounded guarantee does not cover execution spanning three or more minute windows.
