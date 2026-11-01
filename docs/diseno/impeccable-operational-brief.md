# Impeccable Operational Brief — TuHospedaje Frontend Polish

Status: context for planning and review. State-color families and Display typography are implemented in `frontend/src/App.css` and documented in manual v2.1 and `DESIGN.md`. Anti-patterns detected by Impeccable were cleaned across stages 1 to 6 (0 findings reported).

All paths below are relative to the repository root; the frontend is `frontend/`, not a separate filesystem root.

## 1. Required context and authority

Read these documents before proposing a visual change:

1. [Identity Manual v2.1](manual-identidad.md): approved brand identity, official assets, color, Inter, imagery, Lucide, voice and brand accessibility.
2. [Technical design system](../../DESIGN.md): translation of that identity, recorded implementation, adoption debt and explicitly unapproved proposals.
3. The applicable official Sprint criteria and their [traceability matrix](../markdown/compliance/close-user-story-gaps-traceability.md): independent functional constraints. Design documents and tools cannot waive or replace them. Preserve the matrix and historical acceptance evidence.

Impeccable has no brand or product authority. If sources conflict, stop and present the conflict instead of changing requirements or inventing a new rule.

### Integration status

Repository-root `.impeccable/config.json` contains detector configuration, including an Inter exception. That is evidence of local configuration, not proof of the installed tool's complete capabilities, availability in this session, or automatic context loading. Absence of a CLI alone does not establish unavailability.

Before execution, verify the actual host integration and how it receives project context. Explicitly supply this brief and its linked authorities; do not assume any filename is automatically discovered. Do not install packages, change global configuration, or read external installation directories without the required authorization. Until that check is complete, the plan must label Impeccable execution as unverified.

## 2. Preserve the approved identity

### Primary actions and contrast

Preserve the existing `--action-primary-*` contract in `frontend/src/App.css`: petroleum `#264653` background with light `#F4F4F9` text (`9.19:1`), dark `#333333` hover/active background (`11.52:1` with light text), and orange reserved as the action accent (active borders, focus ring offsets, badges, highlights, rating stars, calendar selections; button bottom stripes removed to eliminate UI anti-patterns). Light text on orange is `2.59:1`; white text on orange is `2.84:1`. Neither meets normal-text AA.

Do not infer that the brand's name “primary orange” requires orange CTA backgrounds. Validate essential graphics, focus and text separately on their actual adjacent backgrounds.

### Typography

Keep product-managed Inter and the manual's routine weights `400`, `500`, `600`, `700`. Preserve the existing Inter detector exception; do not suppress unrelated findings. Use existing semantic roles according to meaning, not mechanical search-and-replace. The `16px` continuous-reading minimum is a brand rule, not a universal WCAG minimum. The Display role and `--type-display-*` tokens are implemented in `frontend/src/App.css`.

### Logos and imagery

The application isologotype is `frontend/src/assets/images/TuHospedaje_Isologotipo.png`; manual assets are under `docs/diseno/imagenes/`. There is no `frontend/public/` logo directory. Use the official asset without filtering, recoloring, reconstruction or proportion changes. Preserve the isologotype's `140px` minimum width; measure the manual's `1x` clear space at the rendered size. Existing `8px` padding is not proof of compliance. Other variants have their own minimum sizes in the manual.

Preserve canonical lodging image identities and manifests; visual cleanup does not authorize replacing or generating photographs.

### Scope exclusions

- No dark-mode activation or invented dark palette; residual CSS variables are not approved dark support.
- Preserve Admin's mobile blocking behavior. US #9 requires the exclusion; the `1024px` boundary is a product implementation choice, not a number mandated by the PDF.
- Preserve routes, history-based Back behavior, API calls, state transitions, date availability, authentication and reservation logic.
- Preserve pending/submitting/uploading guards and unsaved-change confirmation in dialogs. Escape and backdrop actions must not bypass them.
- WhatsApp is mounted in Footer's DOM but its wrapper is fixed on desktop and mobile. Do not turn it into normal Footer flow or infer provider delivery from the handoff link.

## 3. Approved context versus pending proposals

| Area | Available basis for planning | Not approved or not implemented |
|---|---|---|
| Color | Five principal brand colors, existing primary-action tokens and the approved state-color extension (`--feedback-*` in `App.css`). | Further palette additions (e.g. dark mode, new brand accents) remain unapproved. |
| Typography | Inter, Display, and H1/H2/Body/Label/Caption roles in `App.css`. | Non-standard font weights (e.g. 300) remain deprecated. |
| Spacing | Existing subset: `--space-2` 8px, `--space-3` 12px, `--space-4` 16px, `--space-6` 24px, `--space-8` 32px; existing radii and shadows. | A complete scale and component-by-component normalization. |
| Navigation | Current destinations, fixed-header clearance, readable link text and tested behavior. | Consistent current-route semantics, menu dismissal/focus improvements and new return-path fallbacks need separately scoped behavior work. |
| Accessibility | Manual's visible focus and `44x44px` product target, plus recorded component contracts. | Product-wide compliance is not established; Home desktop calendar dimensions and other gaps need measurement and bounded corrections. |

The approved feedback colors and normative contrast limits are in manual section 3.4; `DESIGN.md` maps them to canonical CSS tokens defined in `frontend/src/App.css`. The approval preserves the five principal brand colors. Pale borders are approved as decorative only: they fail `3:1` and cannot be essential input/error/focus boundaries. The extension does not specify destructive-button states. The manual and its derived PDF carry the v2.1 decision.

For normal navigation text, water green on petroleum is `3.03:1` and fails `4.5:1`. Preserve light text on petroleum; a graphical accent is not a substitute for readable text or semantic current-page state. The CTA's green focus treatment is not automatically suitable on every surface.

## 4. Future screen-by-screen review

For each bounded area, record the discrepancy, its authoritative rule, implementation status, proposed correction and observable checks before changing anything.

- Review typography, spacing, hierarchy, icon consistency and logo clear space against the existing rules; do not force every view into identical dimensions.
- Review loading, empty, error, success and disabled states without changing their behavior or timing. The recorded `150ms` delay belongs to route loading, not every transition.
- Separate CSS polish from behavioral accessibility fixes. New `aria-current`, menu keyboard handlers or focus restoration require scoped code changes and focused tests.
- Identify apparently unused CSS, then verify imports, dynamic use and tests before proposing removal. This brief does not authorize file deletion.

## 5. Verification and approval boundaries

- **This documentation correction:** check references, statuses, contrast calculations and consistency with the independent audit. No frontend behavior changes, browser acceptance or deployment are claimed.
- **Future implementation:** use focused tests for changed behavior, applicable frontend tests/lint/build, and actual browser checks at representative widths including `320`, `768`, `1024` and `1440px`, with keyboard navigation and component breakpoint boundaries. Use isolated data for flows that can write reservations or send email.
- Passing frontend tests does not approve a new palette or validate normative Markdown. Historical test counts retain their original scope.
- Independent review informs the decision; the human approves new brand decisions. No commit, push, merge, deployment or vault update is authorized by this brief.
