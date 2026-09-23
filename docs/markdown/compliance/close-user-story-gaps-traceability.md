# Academic Requirements Traceability — Phase 2A Reconciliation

**Scope:** official Digital House Sprint 1–4 requirements, reconciled against `product.md` and the revision-bound CI reference `35661433657` at commit `c6f50499b6f7d8fde2567cfae80eab35cc1dad06`.

## Evidence rules

The four original Sprint PDFs are the requirement authority. `product.md` defines the current academic-demo scope. CI run `35661433657` is the current candidate reference: parent verification observed its successful job-level backend, frontend, and browser results. No trace, screenshot, or criterion-by-criterion artifact was used to upgrade every story, so job-level evidence does not prove an individual acceptance criterion by itself. Historical PR #94, PR #233, and earlier-candidate records are context only and are not used as current proof.

Disposition values are `compliant`, `partial`, `missing`, and `non-verifiable`. `partial` means the story is within the current product scope and has the revision-bound CI reference, while one or more acceptance details lack criterion-level observed evidence in this pass.

## Official matrix (US #1–#35)

| US | Official requirement | Scope relation | Disposition | Authority and evidence | Limitation |
| --- | --- | --- | --- | --- | --- |
| 1 | Header/navigation | Academic demo: guest navigation | partial | Sprint 1 PDF; `product.md`; CI `35661433657` @ `c6f50499` | No criterion-level browser artifact inspected. |
| 2 | Branded main with search, categories, recommendations | Academic demo: catalog exploration | partial | Sprint 1 PDF; `product.md`; current CI reference | No layout/browser observation in this pass. |
| 3 | **Register product** | Academic demo: administrator lodging catalog management | partial | Sprint 1 PDF; `product.md`; current CI reference | Product maps to a lodging in the current scope. Product name, description, persistence, duplicate-name handling, and image upload need criterion-level evidence. Upload and canonical image publication are not verified. |
| 4 | Random home products | Academic demo: catalog exploration | partial | Sprint 1 PDF; `product.md`; current CI reference | Randomness, no duplicates, and layout were not independently observed. |
| 5 | Lodging detail | Academic demo: lodging consultation | partial | Sprint 1 PDF; `product.md`; current CI reference | Detail layout/content were not browser-verified here. |
| 6 | Lodging image gallery | Academic demo: lodging consultation | partial | Sprint 1 PDF; `product.md`; current CI reference | Five-image/gallery and responsive behavior were not observed; upload/canonical-image availability remains non-verifiable. |
| 7 | Footer | Academic demo: guest navigation | partial | Sprint 1 PDF; `product.md`; current CI reference | No current responsive/browser artifact inspected. |
| 8 | Home pagination | Academic demo: catalog exploration | partial | Sprint 1 PDF; `product.md`; current CI reference | Page-size, navigation, and reset behavior need criterion-level evidence. |
| 9 | `/administración` administration panel | Academic demo: administrator catalog management | partial | Sprint 1 PDF; `product.md`; current CI reference | Exact route, menu coverage, and mobile unavailable state were not observed in this pass. |
| 10 | List products/lodgings | Academic demo: administrator catalog management | partial | Sprint 1 PDF; `product.md`; current CI reference | Required list columns and administration UI need direct evidence. |
| 11 | Delete product/lodging | Academic demo: administrator catalog management | partial | Sprint 1 PDF; `product.md`; current CI reference | Confirmation, persistence, and cancel path were not observed. |
| 12 | Categorize products/lodgings | Academic demo: administrator catalog management | partial | Sprint 2 PDF; `product.md`; current CI reference | Assignment for new and existing lodgings needs direct evidence. |
| 13 | Register user | Academic demo: registration/login | partial | Sprint 2 PDF; `product.md`; current CI reference | Field validation and user-flow evidence were not inspected. |
| 14 | Identify/login user | Academic demo: registration/login | partial | Sprint 2 PDF; `product.md`; current CI reference | Invalid-login feedback and name/initials avatar need direct evidence. |
| 15 | Log out | Academic demo: guest/anonymous navigation | partial | Sprint 2 PDF; `product.md`; current CI reference | Session invalidation and anonymous continuation were not observed. |
| 16 | Identify/manage administrator | Academic demo: differentiated roles | partial | Sprint 2 PDF; `product.md`; current CI reference | Permission assignment/removal and enforcement need direct evidence. |
| 17 | Manage lodging features | Academic demo: administrator catalog management | partial | Sprint 2 PDF; `product.md`; current CI reference | Feature CRUD and lodging association were not observed. |
| 18 | Show lodging features | Academic demo: lodging consultation | partial | Sprint 2 PDF; `product.md`; current CI reference | Detail block, icons, and responsive behavior need direct evidence. |
| 19 | Registration-confirmation email | Explicitly optional; non-evaluated | non-verifiable | Sprint 2 PDF explicitly says it is optional and not evaluated | No delivery claim is made. |
| 20 | Category filter section | Academic demo: catalog search | partial | Sprint 2 PDF; `product.md`; current CI reference | Multi-select, counts, clearing, and device behavior were not observed. |
| 21 | **Add category** with title, description, and representative image | Academic demo: administrator catalog management | partial | Sprint 2 PDF; `product.md`; current CI reference | Title/description/image CRUD need direct evidence. Upload and canonical representative-image publication are not verified. |
| 22 | Search lodgings | Academic demo: search and availability | partial | Sprint 3 PDF; `product.md`; current CI reference | Suggestions, date range, relevance, and preserved home sections need direct evidence. |
| 23 | Show availability | Academic demo: lodging detail and reservation | partial | Sprint 3 PDF; `product.md`; current CI reference | Calendar, occupied-date indicator, retry, and failure message were not observed. |
| 24 | Mark favorites | Academic demo: favorites | partial | Sprint 3 PDF; `product.md`; current CI reference | Current mobile tests stub the API; real-backend favorite persistence is not verified. |
| 25 | List/manage favorites | Academic demo: favorites | partial | Sprint 3 PDF; `product.md`; current CI reference | Current mobile tests stub the API; cross-section real-time updates and real-backend behavior are not verified. |
| 26 | Show lodging policies | Academic demo: lodging consultation | partial | Sprint 3 PDF; `product.md`; current CI reference | Full-width, underlined title, columns, and policy content were not observed. |
| 27 | Share lodging on social networks | Outside `product.md` explicit demo journeys | non-verifiable | Sprint 3 PDF; current CI reference | UI can only hand off; external provider execution, redirect/integration, and publication are not verifiable here. |
| 28 | Rate completed stay | Academic demo: lodging consultation | partial | Sprint 3 PDF; `product.md`; current CI reference | Eligibility, review rendering, and average-update criteria need direct evidence. |
| 29 | Delete category | Academic demo: administrator catalog management | partial | Sprint 3 PDF; `product.md`; current CI reference | Confirmation/cancel behavior and consequences were not observed. |
| 30 | Select reservation dates | Academic demo: reservation journey | partial | Sprint 4 PDF; `product.md`; current CI reference | Login redirect, date search/results, and unavailable-range protection need direct evidence. |
| 31 | View reservation details | Academic demo: reservation journey | partial | Sprint 4 PDF; `product.md`; current CI reference | Lodging/user/date detail and submit behavior were not observed. |
| 32 | Make reservation | Academic demo: reservation journey | partial | Sprint 4 PDF; `product.md`; current CI reference | Confirmation, optional fields, and specific error paths were not observed. |
| 33 | View reservation history | Academic demo: reservation journey | partial | Sprint 4 PDF; `product.md`; current CI reference | Access, ordering, and displayed reservation data need direct evidence. |
| 34 | Start WhatsApp chat | Outside `product.md` explicit demo journeys; current handoff control retained | partial | Sprint 4 PDF; CI `35661433657` @ `c6f50499`; browser evidence `BE-WA-CI-01` | Current evidence is a handoff/link contract only. External WhatsApp execution, message delivery/read status, and provider-side errors are non-verifiable. |
| 35 | Reservation-confirmation email | Academic demo: reservation confirmation | partial | Sprint 4 PDF; `product.md`; current CI reference | Recipient delivery cannot be claimed without Mailtrap (or equivalent mailbox/provider) evidence. |

## Later increments — not part of the official matrix

US #36–#43 are later increments. They have no corresponding requirement in the four original Sprint PDFs and are intentionally outside the official US #1–#35 reconciliation. They must not be used to fill, replace, or upgrade an official-story disposition.

## Delivery boundary

This work unit updates documentation only. It does not deliver or validate external-provider outcomes, and it does not authorize a commit, push, pull request, or release.
