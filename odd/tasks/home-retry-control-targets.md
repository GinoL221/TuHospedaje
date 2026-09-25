# Home retry control touch targets

## Goal
Complete approved issue #303: give Home search and category error retry controls a consistent style, visible keyboard focus, and at least a 44×44 CSS-pixel interaction area at desktop and narrow mobile widths.

## Boundaries
- Base: `origin/main` at `624bbb3e2d97385c43176339c6e9adfbfe3963bd`.
- Branch: `fix/home-retry-control-targets` in a new isolated worktree. Preserve earlier Home retry worktrees and branches.
- Scope: only the two new Home retry controls, their scoped styling, focused browser regression coverage, and this evidence record. Keep request behavior, valid-empty results, recommendations, service APIs, routing, and formatter configuration unchanged.
- No `.env*` reads, backend/Docker operations, dependency installation, push, PR, merge, or worktree cleanup.

## Task
- [ ] Add a browser regression that fails on the current unstyled retry targets, style the two controls without changing their handlers or alerts, prove desktop/390×844/320×844 focus, size, overflow, and recovery, then close the unit with a signed Conventional Commit.

## Verification plan
- RED then GREEN in isolated Chromium with every API call mocked; avoid the default Playwright config because it loads dotenv. Do not infer manual assistive-technology or provider evidence.
- Run the focused Home tests, full frontend tests/coverage, lint, format check, build, and diff check with existing installed dependencies only.
- Compare the final candidate against the base and review the exact signed work-unit commit when applicable. Record any failed or unavailable check.

## Evidence
- Issue #303 is open with `status:approved`.
- The prior merged main tree shows small native retry buttons; `DESIGN.md` requires focus visibility and at least 44×44px interaction areas. Existing `.recommendations-alert` styling is a visual reference but remains out of scope.
- Source changes are limited to `Home.css` and `e2e/tests/mobile-home.spec.js`; retry handlers, alerts, and recommendations remain unchanged. The browser mock keeps both initial requests failing through React StrictMode until each retry is explicitly enabled; the two known footer icons are stubbed in memory.
- Isolated Chromium with programmatic Vite (`configFile:false`, `envDir:false`) and a temporary Playwright config passed all 3 new failure/retry cases: desktop 1280×844, mobile 390×844, and mobile 320×844. Each asserts 44×44px button bounds, visible focus, viewport containment, recovery to valid empty results, and no horizontal overflow. The initial two-case run used an overly narrow test filter; all three passed after fixing only that temporary filter. No backend/provider or assistive-technology behavior was exercised.
- Focused Home/search tests passed 44/44. The full frontend passed 641/641 across 66 files; coverage passed statements 93.44%, branches 88.71%, functions 89.44%, lines 95.10%. ESLint, frontend Prettier check, production Vite build (2,153 modules), `node --check` for the E2E spec and `git diff --check` passed. Temporary configs/output kept tests and build from reading `.env*` and from writing generated artifacts into the repository.
- Vitest reported pre-existing `act` warnings and Vite reported two unresolved `esbuild` warnings even though tests/build exited zero. Strict test-first RED was not observed; configured TDD mode was not active. No CI or default Playwright config run is claimed.
- Signed work-unit commit and native review pending; no push or PR authorized.
