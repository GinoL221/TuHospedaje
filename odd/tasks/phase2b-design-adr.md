# Phase 2B-D design and ADR tasks

## Scope

Reconcile `DESIGN.md` and the current architecture/ADR document `docs/markdown/project-definition.md` with the normative `docs/diseno/manual-identidad.md` v2.0. Keep the identity manual and its derived PDF unchanged in this unit: they remain the authority, while this work corrects subordinate translations and status claims. Do not change source, tests, workflows, Compose files, Sprint reports, audits, or `product.md`.

## Tasks

- [x] Preserve the authority hierarchy: the identity manual defines brand rules, `DESIGN.md` translates them and records implementation/debt, and project-definition records architecture/ADR context.
- [x] Remove the project-definition claim that the product supports a native dark theme and remove its unapproved dark palette values; state that dark mode is future work and not supported.
- [x] Reconcile ADR 4.6 so Lucide is the current iconography direction and legacy SVG/glyph mechanisms remain implementation debt recorded in `DESIGN.md`.
- [x] Keep `DESIGN.md`'s logo, contrast, Inter, iconography, voice, accessibility, and implementation-status statements subordinate to the identity manual without rewriting historical evidence.
- [x] Preserve current architecture and administrative-table status in project-definition, including the distinction between historical client-side claims and current server-driven areas.
- [x] Validate cross-document consistency, forbidden dark-mode claims, link targets, exact scope, and `git diff --check`.
- [x] Record validation evidence, commit the work unit with a Conventional Commit, and complete the native review gate without pushing or cleaning worktrees.

## Latest evidence

- Kept `docs/diseno/manual-identidad.md` v2.0 and its PDF unchanged as normative authority; subordinate documents now point back to it.
- Removed the project-definition claim of native light/dark support, the unapproved dark palette, and the `data-theme` activation statement. Dark mode is now explicitly future work and unsupported.
- Rewrote ADR 4.6 to make Lucide the current iconography direction and classify inline SVG, Icons8, text glyphs, stars, and other mechanisms as implementation debt.
- Corrected the DESIGN logo status: the manual's proportional `1x` clear-space rule remains normative, while the current fixed `8px` spacing is recorded as pending visual verification rather than equivalent compliance.
- Read-only verification passed: exact four-path scope, relative links, bilingual dark-mode checks, ADR 4.6 direction, DESIGN authority/status checks, administrative-table status, and `git diff --check`.
- No source, test, workflow, Compose, historical report, audit, product, identity-manual, PDF, `.env`, or `.env.example` file was modified or read for secrets. Application tests were intentionally out of scope.
- Signed commit sequence, based directly on `c6f50499`: `9de07252a204a63330337550a7e9d15f663a4172` (`docs: reconcile design and architecture records`) and `02e89082cedaa43298c8c4ac87797c3862f45e68` (`docs: record design ADR evidence`). Signing preserved the final candidate tree byte-for-byte.
- Final native review lineage `review-7db9d2078e98b40c` approved and acknowledged target `sha256:c4b0022effc11e5459d627febd56fd37a752e0b4656f63c02dd3ae0aeb77a282`; `R3-stale-review-target` remained informational and non-blocking.
- Publication revalidation against `origin/main` `87f679b0fd2e11540d1a3168a78ea402b8dd7e1c`: clean/disjoint four-path diff, no merge conflict, seven relative links resolved, identity manual unchanged, content assertions passed, and `git diff --check` passed.

## Acceptance criteria

- No current document in scope claims an approved or implemented dark mode, `data-theme` activation, or a normative dark palette.
- ADR 4.6 references the identity manual's Lucide direction and does not present SVG/Lucide as equal current policy.
- `DESIGN.md` remains a subordinate technical/status document and does not redefine brand authority.
- Existing historical statements remain explicitly historical; the current table status remains intact.
- Relative links in both edited documents resolve in the candidate tree.
- The candidate changes only `DESIGN.md`, `docs/markdown/project-definition.md`, `odd/tasks/phase2b-design-adr.md`, and `odd/phase2b-design-adr/tasks`.

## Verification plan

- Compare authority, dark-mode, Inter, iconography, logo, contrast, voice, and accessibility wording across the three documents.
- Assert the dark palette and `data-theme` claims are absent from current architecture/ADR text.
- Confirm ADR 4.6 points to Lucide and retains legacy alternatives only as debt/context.
- Check relative Markdown links and inspect exact changed paths from `c6f50499`.
- Run `git diff --check`; do not run application tests because this is a documentation-only unit.

## Review and delivery

- Use an isolated branch based directly on `c6f50499`.
- Commit locally only; do not push, open a pull request, merge, or remove worktrees in this unit.
- Run the native review gate for the exact final candidate when the review switch is enabled.
