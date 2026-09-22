# Phase 2B-C root README tasks

## Scope

Replace the root README's duplicated and contradictory setup guidance with a concise project entry point. Keep this work unit limited to `README.md` and these two ODD task artifacts. Do not change Compose files, source, tests, workflows, `DESIGN.md`, historical reports, or environment files.

## Tasks

- [x] State the product purpose, academic scope, current stack, and supported prerequisites without unsupported claims.
- [x] Make the local Docker Compose workflow authoritative: include the required `deploy/dev.env` setup, the base and overlay files, loopback endpoints, and destructive development-only volume cleanup.
- [x] Remove the stale production-future wording and the misleading database-only Compose command from the primary setup path.
- [x] Correct production Compose guidance to use immutable prebuilt image references and remove the unsupported local `compose ... build` step; retain the external TLS and persistent-volume boundaries.
- [x] Keep manual backend/frontend startup, demo-seed, canonical-assets, migration, backup/restore, and operational guidance concise and linked to their source runbooks.
- [x] Keep testing commands and documentation links accurate without duplicating the frontend package README or claiming unavailable external evidence.
- [x] Validate exact scope, relative links, Compose command references, forbidden stale wording, and `git diff --check`.
- [x] Record validation evidence, commit the work unit with a Conventional Commit, and complete the native review gate without pushing or cleaning worktrees.

## Latest evidence

- Replaced the duplicated root README with a concise entry point covering product scope, stack, prerequisites, development Compose, host execution, production operations, tests, and documentation links.
- Development Compose commands consistently use `compose.yaml` plus `compose.dev.yaml`; seed and canonical-assets overlays state their extra requirements.
- Removed the stale production-future wording and the bare `docker compose up -d db` guidance. Host execution now uses the same development overlays.
- Production guidance validates configuration and starts prebuilt `BACKEND_IMAGE`/`FRONTEND_IMAGE` references; it no longer instructs operators to run `compose.prod.yaml build`.
- Read-only verification passed: 18 relative links resolve, 13 Compose commands use the correct overlays, forbidden stale patterns are absent, endpoint-table duplication is absent, and unsupported production-delivery claims are absent.
- Current uncommitted scope is exactly `README.md`, `odd/tasks/phase2b-root-readme.md`, and `odd/phase2b-root-readme/tasks`; `git diff --check` passed.
- `.env` and `.env.example` files were not read or modified; Compose and application tests were intentionally out of scope for this documentation-only unit.
- Work-unit commit: `ec392f49da30b099387988ff41cab03209005513` (`docs: simplify root README`), based directly on `c6f50499`.
- Native review lineage `review-a395429e6eca70a2` approved and acknowledged for target `sha256:039f4b6b849d677a784317cd39b3526fa99dcc8ec725b68af5d238b80a8be84a`; authority burned with `gentle-ai.review-acknowledged/v1`.

## Acceptance criteria

- The root README is a project entry point rather than a duplicate API/catalog reference.
- Local Compose commands use `compose.yaml` with `compose.dev.yaml`; seed and canonical-assets overlays state their extra requirements.
- Production guidance never instructs operators to build images from `compose.prod.yaml`; it uses `BACKEND_IMAGE` and `FRONTEND_IMAGE` references from `deploy/prod.env`.
- No primary setup path relies on `docker compose up -d db` without the development overlay.
- Relative Markdown links resolve in the candidate tree and no `.env` file is read or modified.
- The candidate changes only `README.md`, `odd/tasks/phase2b-root-readme.md`, and `odd/phase2b-root-readme/tasks`.

## Verification plan

- Compare every documented Compose command against the selected Compose files.
- Check all relative Markdown links and the demo-data anchor.
- Search for stale production-future wording, database-only local Compose guidance, and production Compose `build` commands.
- Inspect `git diff --name-only c6f50499..HEAD` and run `git diff --check`.
- Do not run Compose or application tests for this documentation-only unit.

## Review and delivery

- Use an isolated branch based directly on `c6f50499`.
- Commit locally only; do not push, open a pull request, merge, or remove worktrees in this unit.
- Run the native review gate for the exact final candidate when the review switch is enabled.
