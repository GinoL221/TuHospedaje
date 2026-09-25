# Home retry integration evidence

## Objective
Implement approved issue #305 with two focused Playwright cases proving that Home category and search retries reach the real seeded Spring backend after an intercepted initial HTTP 500.

## Problem and rationale
The current mobile Home retry tests mock both failure and recovery. The recommendation retry suite already demonstrates an initial intercepted failure followed by a request to Spring. Add equivalent evidence for category and search while retaining those mocked checks.

## Scope and constraints
- Branch `test/home-retry-real-backend`, base `origin/main` at `f750e0b59a368ccbe8c3dccfe552100433d2712f`; preserve every existing worktree and local change.
- Allowed implementation surface: `e2e/tests/home-retry-real-backend.spec.js` (new). Evidence updates: this document. Do not change CI, frontend handlers or mobile mocks.
- No `.env*` access, credentials, local backend/database/Docker, installation, push, PR, merge or cleanup by inference. Default `e2e/playwright.config.js` loads dotenv and cannot be invoked locally under this constraint.
- A mocked initial failure and real backend recovery establish UI-to-Spring behavior, not a real Spring outage or spoken screen-reader output.
- Technical artifacts and commit messages use English. A local commit requires explicit separate authorization; when authorized, prefer GPG signing and stop on GPG failure before any unsigned commit.

## Execution and tasks
- [ ] **HR-01 — Category retry to real backend.** Add an isolated desktop Playwright case that holds the category request at 500 until its alert appears, removes the route, clicks retry, observes a live 200 array with seeded categories, and matches one returned category name in the Home UI. Route: delegated worker (preparation for writing; independently reviewable behavior). Check: syntax/static verification locally; focused Playwright run in authorized CI environment pending.
- [ ] **HR-02 — Search retry to real backend.** Add an isolated desktop case for a seeded city with initial 500 until search alert, unroute, retry, live 200 with nonempty lodgings and totalItems, and matching result card in the Home UI. Route: delegated worker (preparation for writing and second non-mechanical change). Check: syntax/static verification locally; focused Playwright run in authorized CI environment pending.
- [ ] **HR-03 — Obtain live CI evidence (blocked on separate delivery authorization).** Run the two tests against the seeded CI stack in Chromium and Firefox after a separately authorized commit and push/PR; inspect both projects' results before closing HR-01 and HR-02. Do not start a local stack or read dotenv by inference.

## Acceptance and verification
- Cases must work in the normal desktop projects (Chromium and Firefox); mobile Chromium runs only mobile-*.spec.js, whose mocked retry cases remain unchanged.
- Each live response wait is registered before the retry click. All initial requests are failed through the alert to avoid React StrictMode races. The UI assertion must involve actual backend response data, not just disappearance of the alert.
- Exact local runner: `node --check e2e/tests/home-retry-real-backend.spec.js`, `git diff --check`; CI runner (requires separately authorized push/PR): `cd e2e && npx playwright test tests/home-retry-real-backend.spec.js --project=chromium --project=firefox` on existing CI stack (CI job runs all tests per project).
- Configured TDD mode: not active for this feature, based on previous Home ODD evidence and no active project/session TDD setting; tests are the deliverable. No RED/GREEN assertion is implied.
- Workload forecast: roughly 130–220 authored lines for two cases, plus evidence; delivery strategy `ask-on-risk` below 400-line heuristic. Keep cases as reviewable work units, with Conventional Commit(s) when permitted and verified; record exact identity or explicit blocker.

## Progress and evidence
- Issue #305 is open and independently verified, with `status:approved`.
- Worktree created clean from the merged #304 main commit; only this new untracked spec and the ODD document are changed.
- HR-01 and HR-02 cases are written in `e2e/tests/home-retry-real-backend.spec.js`. Both hold the initial 500 until the alert, unroute before clicking retry, capture an exact GET response registered before the click, assert HTTP 200 and seeded nonempty data, and match a returned name in the DOM. Search also pins `city=Buenos Aires`.
- Independent verification found an initially missing city-query assertion and an overly narrow whitespace check on the untracked file; both were corrected. Final delegated `node --check` passed; `git diff --no-index --check /dev/null e2e/tests/home-retry-real-backend.spec.js` reported no whitespace errors (exit 1 indicates file difference), and tracked `git diff --check` passed.
- Signed work-unit commit: `dd0716af2439297b19cd32bbb9efaaebe4066608` (`test(e2e): cover Home retries against seeded backend`), valid GPG signature. The independent verifier passed `node --check` and `git diff f750e0b..HEAD --check`; the parent repeated the syntax check. Native assessment was unassessable (`schema-incompatible`), so a separate verifier was required; native review lineage `review-23fbfee4c28af43a` approved and acknowledged this exact committed candidate.
- No Playwright browser execution or real backend run has occurred. `e2e/node_modules` is absent in the new worktree; installing dependencies and starting services were not authorized. CI requires separately authorized push/PR; HR-01 and HR-02 remain pending runtime checks. No push or PR has been made. HR-03 records this blocker. This evidence update is separate from the reviewed code candidate.
