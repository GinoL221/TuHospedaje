# WhatsApp Invalid-Configuration Fallback

## Goal

Remove the `href="#"` fallback from the WhatsApp handoff so invalid configuration cannot expose an unintended navigation target.

## Tasks

- [x] Render the WhatsApp link only when the validated handoff URL exists.
- [x] Render an accessible, non-navigating disabled button when configuration is invalid.
- [x] Update focused component and dependent assertions.
- [x] Run frontend checks, native review, and a signed work-unit commit.

## Constraints

- Keep the valid handoff as `https://wa.me/<digits>?text=...` with `target="_blank"` and `rel="noopener noreferrer"`.
- Do not access `.env` files.
- Do not push until explicitly authorized after verification.

## Evidence

- User selected the disabled-button fallback on 2026-09-21.
- Implementation and tests committed with GPG signature as `bbff4e5`.
- Native review `review-211b799d0a679939` was approved and acknowledged.
- Focused tests passed: 2 files, 15 tests; ESLint, Prettier, and `git diff --check` passed.
- The commit hook's Gitleaks scan found no leaks in approximately 4 KB of staged content. E2E remains unrun because it requires environment configuration and external services.
