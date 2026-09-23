# Phase 2B frontend README tasks

## Goal
Replace the Vite-template `frontend/README.md` with accurate onboarding, architecture, API, testing, and environment-variable guidance for the TuHospedaje frontend.

## Scope
- `frontend/README.md`
- `odd/tasks/phase2b-frontend-readme.md`
- `odd/phase2b-frontend-readme/tasks`

Do not modify the root `README.md`, `docs/README.md`, `DESIGN.md`, source code, tests, workflows, Sprint reports, historical evidence, lockfiles, or secrets in this work unit.

## Authority and evidence
- `frontend/package.json` defines supported scripts.
- `frontend/package-lock.json` defines the Node engine constraint and dependency lock state.
- `frontend/src/App.jsx` defines routes and shell boundaries.
- `frontend/src/services/api.js` defines the shared HTTP/CSRF/timeout behavior.
- Frontend source, tests, and configuration prove implementation only for their exact revision.
- Root and documentation READMEs provide repository context but are not edited in this unit.

## Tasks

- [x] Replace the generic Vite README content with TuHospedaje-specific onboarding and scope.
- [x] Document installation, development, production build/preview, lint, test, coverage, and formatting scripts exactly as defined by `frontend/package.json`.
- [x] State the supported Node engine constraint without claiming Node 18 compatibility.
- [x] Document only the observed public environment variable names `VITE_API_URL` and `VITE_WHATSAPP_NUMBER`; never include values or read `.env` files.
- [x] Describe the frontend entry point, routing, authentication guards, component/page/hook/service organization, and shared API behavior with exact paths.
- [x] Describe Vitest/jsdom setup, colocated tests, coverage output/thresholds, and the fact that Playwright E2E lives outside this package.
- [x] Validate the focused documentation diff and record exact evidence.

## Latest evidence

- `frontend/README.md` fully replaces the Vite template with TuHospedaje-specific setup, scripts, environment names, routes, architecture, API behavior, and testing guidance.
- All nine documented npm scripts match `frontend/package.json`; `format` is documented as mutating and `format:check` as non-mutating.
- The README states Vite's locked Node requirement `^20.19.0 || >=22.12.0` and documents only `VITE_API_URL` and `VITE_WHATSAPP_NUMBER` without values.
- All 22 relative Markdown links resolve in the candidate tree; unsupported claims and the generic template are absent.
- Scope verification found only `frontend/README.md`, `odd/tasks/phase2b-frontend-readme.md`, and `odd/phase2b-frontend-readme/tasks` changed.
- `git diff --check` passed. No `.env`, backend secret, or application test was accessed; tests were intentionally out of scope for this documentation-only unit.
- Signed commit sequence, based directly on `c6f50499`: `25b4ab743d3ffaf75891b4127ff18520d659ac1c` (`docs: document frontend package`) and `2915a23d9d6d73277940235f421fff0bfecdf62d` (`docs: record frontend README evidence`). Signing preserved the final candidate tree byte-for-byte.
- Final native review lineage `review-3657430edf189ba3` approved and acknowledged target `sha256:11e7c3b10fe1d9d134965f2c2b29af9db88741fc16e71478f214e94bd46a8805`; `R3-readme-install-dir` remained informational and non-blocking.
- Publication revalidation against `origin/main` `b0ec87f824e3c307c277af943778a34d7b119879`: clean/disjoint three-path diff, no merge conflict, all nine scripts and 22 relative links verified, locked Vite Node requirement confirmed from `frontend/package-lock.json`, content assertions passed, and `git diff --check` passed.

## Acceptance criteria

- `frontend/README.md` contains no Vite template, React Compiler claim, TypeScript migration advice, or unsupported proxy/Node claims.
- Every documented script matches `frontend/package.json`, and `format` is distinguished from non-mutating `format:check`.
- The README lists only `VITE_API_URL` and `VITE_WHATSAPP_NUMBER` without values, credentials, cookies, or `.env` contents.
- Routes, services, API behavior, and test setup match the cited files at the candidate revision.
- The README does not claim production readiness, external delivery, Playwright ownership inside `frontend/`, or fallback behavior for `VITE_API_URL` that the code does not provide.
- Only the scoped README and ODD task artifacts are changed.
- Markdown/link checks and `git diff --check` pass.

## Verification plan

- Assert every documented npm script against `frontend/package.json`.
- Assert every relative Markdown link resolves in the candidate tree.
- Check for forbidden template/unsupported claims and accidental secret/value text without reading `.env` files.
- Inspect the final scope and run `git diff --check`.
- Application tests are not required because this unit changes documentation only.

## Review and delivery

- Commit as a separate Conventional Commit work unit after validation.
- Run the native review gate for the exact candidate when the review switch is enabled.
- Do not push, open a pull request, merge, or clean worktrees in this work unit.
