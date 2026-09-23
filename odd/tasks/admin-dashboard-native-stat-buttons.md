# Admin dashboard native stat buttons

## Goal
Replace simulated dashboard stat-card buttons with native buttons so mouse, Enter, and Space activation use browser semantics without changing tab navigation or visual layout.

## Scope

Production:
- `frontend/src/pages/Admin/AdminDashboard.jsx`
- `frontend/src/pages/Admin/Admin.css`

Tests:
- `frontend/src/pages/Admin/AdminDashboard.test.jsx`

Task evidence:
- `odd/tasks/admin-dashboard-native-stat-buttons.md`
- `odd/admin-dashboard-native-stat-buttons/tasks`

Do not change API calls, dashboard data loading, tab identifiers, other Admin components, routes, mobile scope, environment files, or backend behavior.

## Tasks

- [x] Convert stat cards to native buttons with focused click, Enter, and Space tests while preserving layout styles.
- [ ] Run focused/full frontend verification, record evidence, and complete the native review gate.

## Acceptance criteria

- Every stat card is exposed as a native button with its visible label as accessible name.
- Mouse click, Enter, and Space activate the expected tab exactly once.
- Space uses native button behavior rather than a custom key handler.
- No `role="button"`, manual `tabIndex`, or custom keyboard activation remains on stat cards.
- Existing dashboard loading, error, retry, and recent-table behavior remains unchanged.
- Visual card sizing, alignment, typography, colors, and hover treatment remain explicitly preserved by `.stat-card` styles.
- No environment file or secret is read or changed.

## Verification plan

- `npm --prefix frontend test -- src/pages/Admin/AdminDashboard.test.jsx`
- `npm --prefix frontend run lint`
- `npm --prefix frontend test`
- `git diff --check`
- LSP diagnostics for changed JavaScript files.

## Evidence

### Native stat buttons

- RED: the focused suite failed because cards rendered as `DIV` and Space produced no callback.
- GREEN: `npm --prefix frontend test -- src/pages/Admin/AdminDashboard.test.jsx` passed 15/15 tests.
- Tests confirm native button semantics and exactly one expected tab callback for click, Enter, and Space.
- Independent diff review confirmed `type="button"`, removal of manual role/tabIndex/keydown behavior, unchanged mappings/children, and a four-declaration CSS reset.
- Focused `git diff --check` passed.
- No real-browser or pixel-level visual verification has been run.
