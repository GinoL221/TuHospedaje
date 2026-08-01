# TuHospedaje Technical Design System

This document translates the approved TuHospedaje brand into technical rules and records implementation status and debt. It does not define or override brand identity.

## Quick path

1. Read `docs/diseno/manual-identidad.md` for brand decisions.
2. Check the status in this document before treating a rule as shipped behavior.
3. Verify implementation claims against the production repository.
4. Record exceptions as debt; do not promote them into brand rules.

## Authority and governance

| Priority | Source | Role |
|---:|---|---|
| 1 | `docs/diseno/manual-identidad.md` (Identity Manual v2.0) | Existing primary brand authority for identity, logo, color, typography, imagery, iconography, voice, and brand accessibility. |
| 2 | Published editorial PDF and official assets | Existing approved publication and materialized brand assets. They do not override the manual when a normative difference exists. |
| 3 | `DESIGN.md` | Technical translation of the manual; records product conventions, implementation status, and debt. |
| 4 | Production repository | Source of implemented behavior. Code proves what users receive but does not redefine identity. |

Brand changes require the approval process in [Identity Manual v2.0, section 9](docs/diseno/manual-identidad.md#9-gobernanza). Technical changes subordinate to the manual are recorded here. Do not present future work as implemented.

OpenPencil may produce approved structural directions. Impeccable may refine an accepted direction and support visual or accessibility review. Neither tool has brand authority. Assign file ownership before parallel work.

## Status legend

| Status | Meaning |
|---|---|
| **Implemented** | Verified in the production repository. |
| **Partially implemented** | Present but incomplete, inconsistent, or awaiting stated verification. |
| **Target** | Approved direction that is not implemented. |
| **Not supported** | Explicit current product limitation. |

## Brand translation

The rules in this section come from the identity manual. Product conventions are documented separately.

### Logo

Use only official assets. Preserve proportions, colors, transparency, and composition; never reconstruct, recolor, filter, shadow, outline, crop, or rearrange them.

| Variant | Intended context | Minimum digital size |
|---|---|---:|
| Isologotype | Header, Footer, institutional signature, and wide compositions | `140px` wide |
| Logotype | Horizontal spaces where the brand is already contextualized | `120px` wide |
| Isotype | Favicon, avatar, and constrained spaces; provide a visible or accessible brand name when context is insufficient | `24x24px` |
| Imagotype | Vertical compositions, presentations, and centered placements | `96px` wide |

Maintain at least `1x` clear space on every side, where `x` is the rendered capital `T` height. Allowed backgrounds are white, `#F4F4F9`, uniform photographic areas with clear separation, or another color only after checking the complete asset at final size.

The available assets are transparent and polychromatic. There is no approved monochrome or inverse asset. Do not simulate one with CSS, filters, or recoloring. See [Identity Manual v2.0, section 2](docs/diseno/manual-identidad.md#2-sistema-de-logotipos).

**Implemented:** Header and Footer use the byte-equivalent official `TuHospedaje_Isologotipo.png` asset with automatic aspect ratio, `contain` fitting, a `140px` minimum width, and `8px` clear space. Header exposes an accessible TuHospedaje home name.

### Color and contrast

| Brand role | Value | Intended use |
|---|---:|---|
| Primary orange | `#FF6B35` | Brand action and emphasis |
| Secondary petroleum | `#264653` | Structure, navigation, and text on light backgrounds |
| Accent water green | `#2A9D8F` | Selection and secondary emphasis |
| Light background | `#F4F4F9` | Main light background |
| Dark text | `#333333` | Primary text |

**AA combinations**

| Foreground / background | Ratio | Permitted use |
|---|---:|---|
| Petroleum / light background | `9.19:1` | Normal text |
| Dark text / light background | `11.52:1` | Normal text |
| Orange / dark text | `4.46:1` | Large text or essential graphics only |
| Orange / petroleum | `3.56:1` | Large text or essential graphics only |
| Petroleum / water green | `3.03:1` | Large text or essential graphics only |
| Water green / dark text | `3.80:1` | Large text or essential graphics only |

Do not use orange/water green (`1.17:1`), petroleum/dark text (`1.25:1`), orange/light background (`2.59:1`), or water green/light background (`3.03:1`) for normal text, essential information, or controls. Color must not be the only state signal.

External brand colors are allowed only for the identifiable icon or action of that external service. They are not TuHospedaje palette additions or product tokens. See [Identity Manual v2.0, section 3](docs/diseno/manual-identidad.md#3-paleta-cromática).

**Implementation:** the five brand values exist as global variables. Red danger values remain dispersed and have no canonical token. Dark mode is **not supported**: partial variables exist, but there is no activation path or approved dark brand palette.

### Typography

Inter is the approved brand and product typeface. Authorized routine weights are `400`, `500`, `600`, and `700`.

| Role | Minimum size | Weight | Line height |
|---|---:|---:|---:|
| Display | `40px` | `700` | `1.10` |
| H1 | `32px` | `700` | `1.20` |
| H2 | `24px` | `600` | `1.25` |
| Body | `16px` | `400` | `1.50` |
| Label | `14px` | `500` | `1.40` |
| Caption | `12px` | `400` | `1.40` |

**Implemented:** `@fontsource-variable/inter` `5.3.0` supplies the product-managed variable font through the single `@fontsource-variable/inter/wght.css` import in `main.jsx`. CSS declares `"Inter Variable"`, `"Inter"`, and system fallbacks. There is no remote Google Fonts dependency, and Fontsource supplies `font-display: swap`. The manual scale is still not consistently applied across existing screens.

### Photography

Use authentic, representative lodging images with credible light, balanced color, visible detail, and useful context such as rooms, access, views, and relevant services. Avoid excessive processing, misleading perspective, nonexistent elements, and unverifiable promises. Keep cover, gallery, and description coherent. Lodging-specific canonical identities remain authoritative for their own content. See [Identity Manual v2.0, section 5](docs/diseno/manual-identidad.md#5-fotografía).

### Iconography

Lucide is the approved interface direction. Use recognizable vector icons at `16`, `20`, or `24px`, maintain a consistent stroke within each hierarchy, align icons with text, and provide an accessible label when visible text is absent. Do not use emoji as structural iconography or mix filled and outline styles at the same level. External brand icons may retain their official colors only in their corresponding actions.

**Partially implemented:** Lucide is used in current interface work, but inline SVGs, text glyphs, Icons8 images, stars, and legacy icon mechanisms remain.

### Voice and tone

Product copy should be close, clear, simple, and trustworthy: use neutral Spanish, direct verbs, concrete information, and a next action when one exists. Errors should explain the known problem and recovery path. Avoid vague failures, artificial urgency, blame, and unsupported claims. This is a minimum operational translation; [Identity Manual v2.0, section 7](docs/diseno/manual-identidad.md#7-voz-y-tono) remains authoritative.

## Product conventions

These conventions are derived from the implemented product. They do not come from the identity manual.

### Foundations

| Status | Convention |
|---|---|
| **Partially implemented** | Spacing follows an observed rhythm near a `4px` base, but no complete scale exists. |
| **Implemented** | Recurrent radii are `6px`, `10px`, and `16px`; other local values remain exceptions. |
| **Implemented** | Recurrent shadows are small `0 1px 3px rgba(0,0,0,.08), 0 1px 2px rgba(0,0,0,.06)`, medium `0 4px 12px rgba(0,0,0,.10), 0 2px 4px rgba(0,0,0,.06)`, and large `0 8px 24px rgba(0,0,0,.12), 0 4px 8px rgba(0,0,0,.08)`. Additional shadows are exceptions. |
| **Implemented** | The recurrent transition is `200ms ease`; route loading waits `150ms` to avoid flashing. |
| **Partially implemented** | Reduced-motion coverage is incomplete. |
| **Implemented** | The shared page container is centered, full-width, and capped at `1440px`. |
| **Partially implemented** | Semantic tokens, z-index roles, gutters, fixed offsets, global selectors, and legacy CSS remain fragmented. |

### Responsive behavior

| Status | Width | Current role |
|---|---:|---|
| **Implemented** | `1100px` | SearchResults layout adjustment |
| **Implemented** | `1024px` | Tablet changes and mobile/touch Admin block |
| **Implemented** | `768px` | Primary responsive breakpoint; WhatsApp enters Footer flow |
| **Implemented** | `600px` | Compact SearchResults adjustment |
| **Implemented** | `480px` | Small component adjustments |

Mobile/touch Admin is **not supported**. Touch-capable devices at `<=1024px` receive a blocking message; this is a current limitation, not a design target.

### Components and behavior

| Status | Contract |
|---|---|
| **Implemented** | SearchResults keeps controls available while chips summarize applied filters. Chip removal synchronizes controls while preserving result cards, loading, error, empty, pagination, and responsive behavior; the `600px` layout was manually verified. |
| **Implemented** | One global Footer-mounted WhatsApp action remains fixed on desktop, enters normal Footer flow at `<=768px` with a `48x48px` target, preserves its URL and accessible name, and is hidden when its environment value is absent. |
| **Implemented** | ConfirmDialog has dialog semantics, instance-safe naming, safe initial focus, enabled-control focus containment, return focus, pending-safe Escape and overlay behavior, and duplicate-confirm protection. Its focused tests, Admin regressions, lint, and build were previously recorded as passing. |
| **Implemented** | ShareModal accessibility is verified: dialog semantics, instance-safe name and description, initial Close focus, return focus, keyboard containment including share links, Escape and overlay dismissal, interior-click protection, a named `44px` close target, visible focus, and reduced-motion handling. Social actions preserve external identity through tinted borders and backgrounds while petroleum foregrounds provide at least `6.05:1` contrast across verified light-theme states. URLs, copy, image behavior, `target`, and `rel` are preserved. |
| **Implemented** | ShareModal desktop refinement and mobile presentation at `480x897px` are visually validated. The dialog remains inside the viewport, uses internal scrolling, preserves hierarchy and touch targets, and avoids horizontal overflow. |
| **Implemented** | LodgingFormModal has instance-safe dialog naming and description, deterministic initial focus, enabled-control focus containment, opener-focus restoration after actual close, and safe Escape, overlay, and interior-click behavior. Meaningful scalar, category, feature, policy, and image changes route close requests through ConfirmDialog; cancel restores form focus and confirmed discard closes. The underlying form becomes inert while ConfirmDialog is active, and submit/upload pending guards prevent duplicate submit and unsafe close or discard. Focused component, ImageUpload, ConfirmDialog, and AdminLodgings regressions pass alongside frontend lint and build. |
| **Implemented** | ProductCard uses a declared `400x300` (`4:3`) image and native lazy loading. |
| **Implemented** | Route chunk failures provide a manual page-reload recovery action. |

## Technical accessibility contract

These implementation requirements complement, but do not originate from, the brand manual:

- Keep visible labels and programmatic names, roles, states, and field-error relationships.
- Provide visible keyboard focus and keyboard operation for every control.
- Use at least a `44x44px` interaction area.
- Dialogs require semantics, label relationships, initial and return focus, focus containment, defined overlay and close behavior, and Escape dismissal.
- Announce introduced errors and status updates when needed.
- Respect `prefers-reduced-motion`; current product-wide coverage is partial.

SearchResults, ConfirmDialog, ShareModal, and LodgingFormModal have the local accessibility coverage recorded above. Product-wide focus, touch-target, error-association, and reduced-motion coverage remains partial.

## Known brand deviations

- [ ] **Mixed legacy iconography:** Lucide coexists with inline SVG, Icons8 images, text glyphs, stars, and legacy mechanisms.
- [ ] **Primary button contrast:** verified CSS uses white normal text on primary orange backgrounds in multiple CTA styles. This `2.59:1` pair contradicts Identity Manual v2.0 and must not be treated as an approved brand combination.

## Other technical debt

- [ ] Raw colors, shadows, spacing, radii, dimensions, fixed offsets, and breakpoints remain hardcoded or fragmented.
- [ ] `--bg` is overloaded across backgrounds, surfaces, and states; danger and layer tokens are incomplete.
- [ ] Global selectors and legacy CSS can collide across features.
- [ ] Focus-visible, touch-target, semantic error association, and reduced-motion coverage remain incomplete.
- [ ] Dark mode is not supported.
- [ ] Mobile/touch Admin is not supported.

## Adoption path

- [x] Preserve the completed SearchResults state and responsive slice.
- [x] Preserve the completed WhatsApp/Footer behavior slice.
- [x] Preserve ConfirmDialog accessibility behavior.
- [x] Preserve ShareModal accessibility behavior.
- [x] Preserve the verified identity work unit: local Inter Variable loading and official Header/Footer isologotype usage. Its focused 25 tests, lint, and build passed.
- [x] Preserve the visually validated ShareModal desktop and `480x897px` mobile presentation.
- [x] Preserve LodgingFormModal accessibility and unsaved-change coordination with ConfirmDialog.
- [ ] Apply the manual typography scale consistently across existing screens.
- [ ] Correct primary CTA contrast through separately approved implementation work.
- [ ] Consolidate semantic tokens and component states incrementally; do not introduce another palette or infer brand rules from existing exceptions.
