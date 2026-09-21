# WhatsApp Invalid-Configuration Fallback

## Goal

Remove the `href="#"` fallback from the WhatsApp handoff so invalid configuration cannot expose an unintended navigation target.

## Tasks

- [ ] Render the WhatsApp link only when the validated handoff URL exists.
- [ ] Render an accessible, non-navigating disabled button when configuration is invalid.
- [ ] Update focused component and dependent assertions.
- [ ] Run frontend checks, native review, and a signed work-unit commit.

## Constraints

- Keep the valid handoff as `https://wa.me/<digits>?text=...` with `target="_blank"` and `rel="noopener noreferrer"`.
- Do not access `.env` files.
- Do not push until explicitly authorized after verification.

## Evidence

- User selected the disabled-button fallback on 2026-09-21.
