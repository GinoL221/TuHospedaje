# City autocomplete request cancellation

## Goal
Prevent stale city-autocomplete requests from overwriting newer input state while preserving the current debounce, selection, keyboard, loading, and real-error behavior.

## Scope

Production:
- `frontend/src/services/api.js`
- `frontend/src/services/lodgingService.js`
- `frontend/src/hooks/useCityAutocomplete.js`

Tests:
- `frontend/src/services/api.test.js`
- `frontend/src/services/lodgingService.test.js`
- `frontend/src/hooks/useCityAutocomplete.test.js`

Task evidence:
- `odd/tasks/city-autocomplete-request-cancellation.md`
- `odd/city-autocomplete-request-cancellation/tasks`

Do not change `Home.jsx`, backend code, routes, UI copy, workflows, environment files, or unrelated frontend behavior.

## Technical contract

- The shared GET helper accepts an optional external `AbortSignal` without changing existing one-argument callers.
- A caller-requested abort remains distinguishable from the existing request-deadline timeout.
- `getCities` forwards the optional signal.
- The autocomplete owns one active request at a time and invalidates it when a newer search starts, the input becomes shorter than two characters, a city is selected, or the hook unmounts.
- Only the current request may update suggestions, the active option, or loading state.
- Cancellation is silent. Non-cancellation failures preserve the current empty-list behavior.

## Tasks

- [x] Add the optional signal contract to the API and lodging service with focused red/green tests.
- [x] Cancel and invalidate stale autocomplete requests with focused race, short-input, selection, and unmount tests.
- [x] Run focused and full frontend verification, record evidence, and complete the native review gate.

## Acceptance criteria

- Two deferred city requests resolved out of order leave the newest result visible.
- Resolving or rejecting an invalidated request cannot clear or replace current state.
- Short input, selection, and unmount abort an active city request.
- Caller aborts do not become timeout errors; deadline aborts retain the current timeout message.
- Existing GET callers remain source-compatible.
- Existing debounce, keyboard, blur, selection, loading, and real-error tests continue to pass.
- No environment file or secret is read or changed.

## Verification plan

- `npm --prefix frontend test -- src/services/api.test.js src/services/lodgingService.test.js src/hooks/useCityAutocomplete.test.js`
- `npm --prefix frontend run lint`
- `npm --prefix frontend test`
- `git diff --check`
- LSP diagnostics for all changed JavaScript files.

## Evidence

### Request signal contract

- RED: focused API/service tests failed in three places because GET ignored `signal`, caller aborts became timeout errors, and `getCities` did not forward the signal.
- GREEN: `npm --prefix frontend test -- src/services/api.test.js src/services/lodgingService.test.js` passed 52/52 tests.
- Independent verification confirmed one-argument compatibility, caller-abort distinction, deadline timeout preservation, signal continuity across 401 refresh/retry, and `getCities` forwarding.
- `git diff --check` passed for the four task paths.
- Signed work-unit commit: `df90417880858064a748a453c86659d2814441c0` (`fix: propagate request cancellation signals`).
- The worktree reuses the root frontend dependency installation through a local ignored symlink only after verifying identical `package-lock.json` hashes; no packages were installed.

### Autocomplete lifecycle

- Focused hook verification passed 11/11 tests.
- The tests cover out-of-order deferred responses, a transport that resolves after abort, active-request cancellation for short input/selection/unmount, cancellation silence, and loading ownership.
- Independent diff review confirmed one active request, current-request identity guards, preserved debounce/blur/selection/keyboard behavior, and unchanged real-error empty-list behavior.
- `git diff --check` passed for the hook and its tests.
- Signed work-unit commit: `02d87d968b1d431fe2381b3766850f07b49a8ad0` (`fix: cancel stale city autocomplete requests`).

### Final verification

- Focused candidate tests passed 63/63 across the API, lodging service, and autocomplete hook suites.
- Full frontend tests passed 629/629 across 66 files.
- Frontend ESLint passed with no findings.
- `git diff --check` passed for the committed candidate and the pending evidence update.
- LSP probing reported no diagnostics, but all six files remained unconfirmed because the server is silent on a clean re-check.
- Browser, provider, and backend tests were not run because this unit changes only frontend request/autocomplete behavior and has no provider or backend boundary.
- Signed verification evidence commit: `65180de3c86ef54a1d10730fe80a90f8c0bae819` (`test: record autocomplete cancellation verification`).
- Native review `review-f5fbf4d629258310` approved and acknowledged target `sha256:42b734a0a11251a2166da32de8285f3cd071d06f8a6dcc561205d1e33903e1a4`; authority burned with `gentle-ai.review-acknowledged/v1`.
