# Home search accessible names

## Goal
Give the Home city combobox and both date inputs stable accessible names independent of their placeholders, without changing the search layout or behavior.

## Scope

- `frontend/src/pages/Home/Home.jsx`
- `frontend/src/pages/Home/Home.test.jsx`
- `odd/tasks/home-search-accessible-names.md`
- `odd/home-search-accessible-names/tasks`

Keep placeholders, locale, city autocomplete semantics, date picker behavior, query parameters, CSS, and mobile selectors unchanged. Do not change search/category error UX or other frontend debt.

## Tasks

- [x] Add explicit accessible names to city and date inputs with tests that fail before implementation and use the datepicker's real `ariaLabel` prop.
- [x] Verify focused/full frontend checks and native review; record results and commits.

## Acceptance criteria

- City input is a combobox named `Ciudad` whether empty or populated.
- Date inputs are textboxes named `Check-in` and `Check-out` even when values replace placeholders.
- The DatePicker mock forwards `ariaLabel` to its input independently of `placeholderText`.
- Existing placeholder strings, Spanish locale, autocomplete/listbox attributes, submission, and layout remain unchanged.
- No `.env*`, credentials, or secrets are accessed.

## Verification plan

- Focused Home component tests, then full frontend tests and ESLint.
- `git diff --check`; LSP diagnostics on changed JavaScript files.
- Native review of the completed candidate before reporting it complete.

## Evidence

- RED: two focused assertions failed for unnamed city and date inputs after the mock stopped borrowing names from placeholders.
- GREEN: focused Home suite passed 29/29; independent verification repeated 29/29.
- Independent review of the 2-file diff (+38/-3) confirmed city `aria-label` and DatePicker `ariaLabel` props, unchanged placeholders, locale, autocomplete, and submission.
- Focused `git diff --check` passed.
- Signed work-unit commit: `855b02c75ccfb824e07d5a048f50d48bd13762d9` (`fix: name home search inputs`).
- Full frontend verification passed: ESLint and 631/631 tests across 66 files; `git diff --check HEAD` passed.
- LSP reported seven informational spelling suggestions for existing Spanish text/test values, with no error or warning diagnostics.
- Real-browser verification was not run.
- Signed verification commit: `28f12e14ca4d9acd0918a399c6f96f78102790e2` (`docs: record home search verification`).
- Native review `review-1e4e50928519f5ad` approved and acknowledged target `sha256:1362e04443fc7f77002c006aca0cceca27c5c321fe28e7a58280811bd7cba628`.
