# WhatsApp Invalid-Configuration Fallback

## Goal

Honor US #34 while removing the `href="#"` fallback from the WhatsApp handoff: only a validated link is rendered, and invalid configuration renders no WhatsApp control.

## Tasks

- [x] Render the WhatsApp link only when the validated handoff URL exists.
- [x] Keep the valid handoff as a declarative link with its preloaded message.
- [x] Render no WhatsApp control when configuration is missing or invalid, as required by US #34.
- [x] Update focused component and dependent assertions.
- [ ] Run frontend checks, native review, and a work-unit commit.

## Constraints

- Keep the valid handoff as `https://wa.me/<digits>?text=...` with `target="_blank"` and `rel="noopener noreferrer"`.
- Do not access `.env` files.
- Do not push until explicitly authorized after verification.

## Evidence

- The earlier disabled-button fallback was superseded after reconciling issue #277 with the authoritative US #34 acceptance criteria.
- The previous implementation and tests were committed with GPG signature as `bbff4e5` and reviewed under `review-211b799d0a679939`.
- Focused tests, Prettier, ESLint, and `git diff --check` pass for the correction; native review and a new work-unit commit remain pending.
