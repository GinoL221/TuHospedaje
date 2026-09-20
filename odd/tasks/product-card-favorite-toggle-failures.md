# ProductCard favorite toggle failures

## Scope

Make failed ProductCard favorite add/remove requests visible and retryable without changing successful optimistic behavior or backend contracts.

## Issue

- GitHub issue: #273 (approved, `type:bug`)
- Branch: `fix/product-card-favorite-errors`
- Base: `origin/main` at merge commit `986a7512f9c2dd0a8f72b66c9323218cd6f4f2b6`

## Tasks

- [x] Confirm the current ProductCard failure path, duplicate-request guard, parent callback, and existing coverage.
- [x] Add visible item-scoped failure feedback and retry behavior for add/remove failures.
- [x] Extend focused ProductCard tests for both failure directions, retry, error clearance, and pending protection.
- [x] Run focused and repository-required frontend verification.
- [ ] Complete native review, create a signed work-unit commit, push, and open the issue-linked PR.

## Constraints

- Keep the existing optimistic success behavior.
- Failed add returns to unfavorited; failed remove returns to favorited.
- Keep the pending-request duplicate guard and parent synchronization.
- Do not change backend favorite endpoints, FavoritesPage removal feedback, or generic API behavior.
- Preserve unrelated local changes in `frontend/src/accountSharedAuditContract.test.jsx` and `frontend/src/services/lodgingService.js`.

## Acceptance criteria

- Failed add/remove requests show an item-scoped `role="alert"` and re-enable the favorite control.
- A retry clears stale failure feedback when it starts and removes it after success.
- Pending requests remain single-flight and expose busy state.
- Existing successful add/remove tests remain green.

## Evidence

- Exploration: current `ProductCard.jsx` caught errors with `console.error`, cleared optimistic state, and emitted only the rollback callback.
- Implementation: `ProductCard.jsx` now exposes a Spanish `role="alert"` and clears the error on retry; `ProductCard.css` adds the item-scoped presentation.
- Focused verification: ProductCard suite — 17 passed; ESLint — passed; `git diff --check` — passed.
- Full frontend verification: 66 test files and 623 tests passed; coverage — 93.48% statements, 88.46% branches, 89.01% functions, 95.12% lines.
- Delegated verifier stopped on a false external-format warning after the focused test; parent completed the remaining read-only checks locally.
- Commit: pending.
