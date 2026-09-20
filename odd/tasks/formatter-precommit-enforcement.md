# Formatter and pre-commit enforcement tasks

## Scope

Add deterministic frontend formatting with Prettier, preserve the existing Gitleaks pre-commit protection, and enforce formatting in CI. Do not format the E2E package or alter unrelated working-tree changes.

## Tasks

- [x] Add the frontend Prettier configuration, scripts, dependencies, and staged-file hook integration while preserving Gitleaks.
  - Evidence: `frontend/.prettierrc.json`, `frontend/.prettierignore`, `frontend/scripts/pre-commit.sh`, `frontend/package.json`, and `frontend/package-lock.json`.
  - Dependencies: Prettier 3.9.6, lint-staged 16.4.0 (Node 20 compatible), simple-git-hooks 2.14.0.
  - Validation: dependency/lockfile checks and `sh -n frontend/scripts/pre-commit.sh` passed; the initial source baseline had 136 Prettier violations.
- [x] Add frontend lint and format checks to CI.
  - Evidence: `.github/workflows/ci.yml` runs `npm run lint` and `npm run format:check` after `npm ci` and before coverage in the frontend job.
  - Validation: `git diff --check` passed; workflow diff contains only the two intended steps.
- [x] Apply and verify the separate frontend formatting baseline while preserving local changes.
  - Evidence: 135 tracked frontend source files were formatted: 124 initial baseline files plus 11 omitted root-level files.
  - Compatibility adjustment: `frontend/src/publicShellContract.test.js` received one regex-only adjustment for Prettier's legal multiline CSS whitespace.
  - Preserved local files: the original local changes in `frontend/src/accountSharedAuditContract.test.jsx` and `frontend/src/services/lodgingService.js` were restored unchanged. The existing `frontend/src/components/ProductCard/ProductCard.css` change remains intact.
  - Clean baseline validation passed: lint, full `format:check`, coverage for 66/66 files and 623/623 tests, hook installation/configuration checks, JSON/lockfile checks, and `git diff --check`.
- [ ] Track the deferred WhatsApp open-redirect finding as a separate security task.
  - Scope boundary: this finding remains separate from the formatter work and needs separate security remediation and validation.
- [x] Prepare the clean formatter baseline and commit candidate while preserving unrelated local changes.
  - Native review inspect/start reached the provider, but no review authority was created because the complete 141-file candidate exceeded the provider reviewer context budget (`lens_context_budget_exceeded`).
  - The candidate must be split into smaller reviewable work units/commits before review.
- [ ] Commit the completed formatter work unit after explicit commit authorization.
  - Commit authorization is still pending.

## Constraints

- Keep `frontend/src/accountSharedAuditContract.test.jsx`, `frontend/src/services/lodgingService.js`, and the existing `ProductCard.css` working-tree changes intact unless the formatter changes are explicitly part of this feature.
- Do not read or modify any `.env` file.
- Use one formatter for frontend files; keep ESLint for semantic/style rules.
- Do not claim hook installation, CI execution, staging, or a commit without observed authorization and evidence.

## Evidence

- Investigation: `frontend/package.json` has ESLint and tests but no formatter or hook manager.
- Investigation: `.github/workflows/ci.yml` runs frontend coverage but no lint or format check.
- Investigation: local `.git/hooks/pre-commit` runs Gitleaks and must be preserved or chained.
