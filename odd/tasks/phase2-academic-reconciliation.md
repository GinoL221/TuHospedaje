# Phase 2A academic traceability reconciliation

## Goal

Update the compliance traceability and browser-evidence documents so the official Digital House US #1–#35 requirements, current `product.md` scope, and revision-bound evidence are reconciled without promoting historical or provider-bound claims to current proof.

## Scope

- `docs/markdown/compliance/close-user-story-gaps-traceability.md`
- `docs/markdown/compliance/close-user-story-gaps-browser-evidence.md`

## Authority

- Original Digital House PDFs supplied by the user: Sprint 1–4.
- `product.md` for current academic scope and demo definition.
- Current code, tests, and CI run `35661433657` for revision-bound implementation evidence.
- Existing dated reports remain contextual evidence only.

## Constraints

- Keep official US #1–#35 separate from later increments US #36–#43.
- Preserve the explicit optional/non-evaluated disposition of US #19.
- Resolve US #3 as product/lodging registration and US #21 as category creation with title, description, and representative image.
- Do not claim Mailtrap delivery, WhatsApp delivery/read status, canonical JPEG publication, or real-backend favorites E2E without observed evidence.
- Replace stale WhatsApp pending/older-candidate claims with current CI evidence while preserving the external handoff boundary.
- Do not access `.env` or backend secrets.
- Do not modify source code, tests, workflows, Sprint reports, or historical evidence artifacts in this work unit.

## Tasks

- [x] Build the official US #1–#35 matrix with authority, scope relation, disposition, evidence, and limitations.
- [x] Mark US #36–#43 as later increments outside the official 35-story matrix.
- [x] Reconcile current CI run `35661433657` on commit `c6f50499b6f7d8fde2567cfae80eab35cc1dad06` with the browser-evidence document.
- [x] Correct WhatsApp evidence wording and retain its handoff-only provider boundary.
- [x] Record partial/non-verifiable boundaries for uploads/images, favorites stubs, external share/WhatsApp execution, and reservation email recipient delivery.
- [x] Run focused documentation validation and review the final diff.

## Acceptance criteria

- Every official US #1–#35 has one explicit disposition: compliant, partial, missing, or non-verifiable/out-of-scope, with a source and evidence reference.
- US #19 is explicitly optional and non-evaluated.
- No current claim depends on the stale PR #94/PR #233/older-candidate evidence without being labeled historical.
- Current WhatsApp CI evidence is revision-bound to `c6f50499` and does not claim external delivery.
- The documents remain internally consistent with `product.md`, the original PDFs, and the current CI boundary.
- `git diff --check` and applicable Markdown/format checks pass.

## Verification evidence

- Current main CI: GitHub Actions run `35661433657` at `c6f50499b6f7d8fde2567cfae80eab35cc1dad06`; observed job-level results: backend 651, frontend 621, Chromium 58, Firefox 58, mobile Chromium 31; two mobile booking scenarios passed on retry.
- Original authority: user-provided `Sprint 1.pdf` through `Sprint 4.pdf`; final verification matched US #3 and US #21 wording and criteria.
- Python reconciliation assertions: passed (35 unique rows, optional US #19, separated US #36–#43, CI reference, and evidence boundaries).
- `git diff --check`: passed.
- Final delegated verification: passed; no remaining issues.
- Local Playwright is deferred because required credentials/services are unavailable.
- Signed work-unit commit: `8473fa4179242cc7ae5a415f217c697d6ca0bb4b` (`docs: reconcile academic traceability evidence`); its tree is byte-identical to the candidate approved by native review `review-d4f64acd712d4f0d`.
- Publication revalidation against `origin/main` `c48345f21f6270b4138581b17622f4bcc936b657`: clean worktree, disjoint four-path diff, no merge conflict, documentation assertions passed, and `git diff --check` passed.
