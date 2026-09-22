# Phase 2B documentation index tasks

## Goal
Create `docs/README.md` as an authority-aware navigation index for the repository documentation without duplicating requirements, brand rules, implementation claims, or operational policy.

## Scope
- `docs/README.md`
- `odd/tasks/phase2b-docs-index.md`
- `odd/phase2b-docs-index/tasks`

Do not modify the root `README.md`, `frontend/README.md`, `DESIGN.md`, `docs/markdown/project-definition.md`, source code, tests, workflows, Sprint reports, historical evidence, or secrets in this work unit.

## Authority
- Original Digital House Sprint 1–4 PDFs supplied by the user govern official user stories.
- `product.md` governs current academic scope and demonstration journeys.
- `docs/diseno/manual-identidad.md` v2.0 governs brand and visual identity.
- `DESIGN.md` translates identity and architecture decisions technically but is subordinate to the identity manual.
- Code, tests, and CI prove implementation only for an exact revision and environment.
- Reports, Sprint plans, audits, compliance matrices, browser evidence, and ODD artifacts are dated/contextual evidence unless explicitly tied to a revision.

## Tasks

- [x] Create `docs/README.md` with a concise navigation purpose and a non-normative index disclaimer.
- [x] Document the source-of-truth hierarchy and distinguish normative guidance, technical guidance, historical reports, and revision-bound evidence.
- [x] Link to current scope, design authority, architecture/ADR context, compliance evidence, audits, migrations, Sprint materials, and deliverable PDFs using valid repository-relative paths.
- [x] Call out evidence boundaries without claiming Mailtrap delivery, canonical JPEG publication, production backup/restore, dark-mode support, or current CI beyond the cited revision.
- [x] Validate links and Markdown structure without reading or requiring `.env` or backend secrets.
- [x] Review the focused diff and record the exact validation evidence.

## Latest evidence

- `docs/README.md` created with the authority hierarchy, navigation categories, historical/evidence boundaries, and external Digital House PDF qualification.
- Relative Markdown, PDF, and directory links introduced by the index all resolve in the candidate tree.
- `git status --short` shows only `docs/README.md`, `odd/tasks/phase2b-docs-index.md`, and `odd/phase2b-docs-index/tasks` as intended untracked paths.
- `git diff --check` passed.
- No `.env` or backend secret was inspected; application tests were not required for this documentation-only unit.
- Clean candidate work-unit commit: `c7026dbf89b789ec538f7d4dd64f44717d99a74d` (`docs: add documentation index`), based directly on `c6f50499`.
- Native review lineage `review-23fd8a2a67f75c89` approved and acknowledged for the final exact three-path candidate `sha256:21dc64f36a55d6ff9751f9c377782b861d2f856d0a936354cdd68f75d45417a8`; authority burned with `gentle-ai.review-acknowledged/v1`.

## Acceptance criteria

- `docs/README.md` exists and contains no duplicated US matrix, brand policy, or implementation contract.
- Every linked target exists in the candidate tree or is explicitly identified as an external/user-supplied authority.
- Current versus historical/evidence-only documentation is visibly separated.
- The index links upward to `product.md`, `docs/diseno/manual-identidad.md`, `DESIGN.md`, `docs/markdown/project-definition.md`, compliance, audits, Sprint documentation, migrations, and deliverables where present.
- The document does not state that the project is production-ready or that external delivery/backup/image claims are proven.
- Only the scoped documentation and ODD task artifacts are changed.
- Markdown/link checks and `git diff --check` pass.

## Verification plan

- Check every relative Markdown target with a repository-local script or equivalent read-only assertions.
- Inspect the final diff for scope, hierarchy, contradiction, and stale evidence claims.
- Run `git diff --check`.
- Local application tests are not required because this work unit changes documentation only.

## Review and delivery

- Commit as a separate Conventional Commit work unit after validation.
- Run the native review gate for the exact candidate when the review switch is enabled.
- Do not push, open a pull request, merge, or clean worktrees in this work unit.
