# Home retry control touch targets

## Goal
Complete approved issue #303: give Home search and category error retry controls a consistent style, visible keyboard focus, and at least a 44×44 CSS-pixel interaction area at desktop and narrow mobile widths.

## Boundaries
- Base: `origin/main` at `624bbb3e2d97385c43176339c6e9adfbfe3963bd`.
- Branch: `fix/home-retry-control-targets` in a new isolated worktree. Preserve earlier Home retry worktrees and branches.
- Scope: only the two new Home retry controls, their scoped styling, focused browser regression coverage, and this evidence record. Keep request behavior, valid-empty results, recommendations, service APIs, routing, and formatter configuration unchanged.
- No `.env*` reads, backend/Docker operations, dependency installation, further push, merge, or worktree cleanup. PR #304 is open; correction is local until separately authorized for push.

## Task
- [x] Style the two controls without changing their handlers or alerts, cover desktop/390×844/320×844 focus, size, overflow, and recovery, and close the unit with a signed Conventional Commit. A pre-change RED run was not observed.
- [ ] Address the review finding in PR #304: ensure light-theme alert text and retry buttons (normal and hover) meet 4.5:1 text contrast, add a browser assertion for computed colors, preserve sizing/focus/request behavior, run isolated browser and frontend checks, then create one signed local work-unit commit. No push or merge is authorized for this correction.

## Verification plan
- Run isolated Chromium with every API call mocked; avoid the default Playwright config because it loads dotenv. Do not infer manual assistive-technology or provider evidence.
- Run the focused Home tests, full frontend tests/coverage, lint, format check, build, and diff check with existing installed dependencies only.
- Compare the final candidate against the base and review the exact signed work-unit commit when applicable. Record any failed or unavailable check.

## Evidence
- Issue #303 is open with `status:approved`.
- The prior merged main tree shows small native retry buttons; `DESIGN.md` requires focus visibility and at least 44×44px interaction areas. Existing `.recommendations-alert` styling is a visual reference but remains out of scope.
- Source changes are limited to `Home.css` and `e2e/tests/mobile-home.spec.js`; retry handlers, alerts, and recommendations remain unchanged. The browser mock keeps both initial requests failing through React StrictMode until each retry is explicitly enabled; the two known footer icons are stubbed in memory.
- Isolated Chromium with programmatic Vite (`configFile:false`, `envDir:false`) and a temporary Playwright config passed all 3 new failure/retry cases: desktop 1280×844, mobile 390×844, and mobile 320×844. Each asserts 44×44px button bounds, visible focus, viewport containment, recovery to valid empty results, and no horizontal overflow. The initial two-case run used an overly narrow test filter; all three passed after fixing only that temporary filter. No backend/provider or assistive-technology behavior was exercised.
- Focused Home/search tests passed 44/44. The full frontend passed 641/641 across 66 files; coverage passed statements 93.44%, branches 88.71%, functions 89.44%, lines 95.10%. ESLint, frontend Prettier check, production Vite build (2,153 modules), `node --check` for the E2E spec and `git diff --check` passed. Temporary configs/output kept tests and build from reading `.env*` and from writing generated artifacts into the repository.
- Vitest reported pre-existing `act` warnings and Vite reported two unresolved `esbuild` warnings even though tests/build exited zero. Strict test-first RED was not observed; configured TDD mode was not active. No CI or default Playwright config run is claimed.
- Signed work-unit commit: `ad64b9ce7326a7701521228fa981000f7df19e53` (`git show` reports a valid GPG signature). Native review lineage `review-a9dfe5414aa2a823` approved and acknowledged for that exact candidate; authority burned. Native `assess` remained unassessable (`schema-incompatible`), so an independent verifier ran the browser, unit/coverage, lint, formatting, and build checks above.
- PR #304 was opened from `592bc8bc50cb0e911569a78e9daf10db2bb88acf` with `Closes #303` and `type:bug`; all six CI jobs passed for that published candidate. A subsequent independent review found that light-theme `--primary` text on `--bg` and the new buttons gives approximately 2.6:1 contrast, below 4.5:1. Correction is authorized locally with one signed commit, but has not yet been implemented or pushed. The approved native review is bound to the previous code commit, not to this pending correction.
- Review correction TDD: isolated Chromium RED failed in all 3 viewports on computed alert-text contrast `2.8353525675311277`; after the scoped CSS token change, GREEN passed 3/3 at desktop/390/320 with computed 4.5:1-or-higher assertions for both alert texts and both retry buttons in normal and hover states. Existing 44×44px, focus, overflow and independent retry-to-empty assertions still passed. This does not prove provider or assistive-technology behavior.
- Independent verification of the local correction passed: the same isolated browser 3/3; full frontend 641/641 across 66 files; coverage statements 93.44%, branches 88.71%, functions 89.44%, lines 95.10% (all thresholds met); ESLint, frontend Prettier check, Vite production build (2,153 modules), E2E syntax check and `git diff --check`. Known React `act` and unresolved `esbuild` warnings remain. Tests/build used temporary configs with `envDir:false`; the default Playwright/Vite configs were not used.
- Review correction still needs its one signed local commit and candidate-bound native review. Further push, merge, worktree cleanup, real-provider and assistive-technology checks remain unperformed or unauthorized.
