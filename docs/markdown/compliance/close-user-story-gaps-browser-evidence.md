# Browser and CI Evidence — Current Academic Candidate

## Candidate identity and evidence boundary

**Target candidate:** main commit `76673879a53ddc560436c2e7dcfe0609eb2569f1`.

The task record reports GitHub Actions run **`36210483665`** at that SHA with six successful jobs. This reconciliation did not inspect the run's individual logs, browser artifacts, traces, screenshots, or per-test outcomes. Record it as a job-level success reference only. It does **not** prove any individual Digital House acceptance criterion.

The current traceability result is therefore: **no candidate-linked criterion-by-criterion browser artifact was available for inspection in this work unit**. Sprint test plans and sprint reports document intended/manual test cases and historical “Pasa/Verificado” results; they do not identify this exact candidate/run as their execution environment. A listed test or source file is not proof that it ran on this SHA or covered every official acceptance detail. The evidence gaps do not confirm implementation omissions; no implementation gap is confirmed by available criterion-level evidence.

## Evidence inventory

| Evidence ID | Candidate/scope | Observation | Result boundary |
|---|---|---|---|
| BE-CANDIDATE-01 | Target `main@76673879a53ddc560436c2e7dcfe0609eb2569f1`; docs branch `docs/academic-story-reconciliation`, worktree HEAD `e4a7bfad2b2aa4935a1a9e9a722c0a54e31390a5` | The docs worktree HEAD is distinct from the target. The task record reports CI `36210483665` as six-job success only. | No CI logs or browser artifacts were inspected. Candidate identity and task-reported job-level status only; no criterion-level result is established. |
| BE-SPRINT-PLANS-01 | Sprint 1–4 | `docs/markdown/sprint-{1,2,3,4}/sprint-*-test-plan.md` lists cases TC-01 through TC-35 and historical result labels. | Documentation of planned/reported checks, not a run bound to BE-CANDIDATE-01. Sprint 4 report references another historical CI candidate. |
| BE-HISTORICAL-RECON-01 | `c6f50499b6f7d8fde2567cfae80eab35cc1dad06` / CI `35661433657` | Earlier compliance documents report successful jobs, WhatsApp contract checks, and limitations for favorites, images, sharing, and email. | Historical candidate only. Do not attribute to BE-CANDIDATE-01. |
| BE-FAV-BOUNDARY-01 | US #24–#25, prior reconciliation | Earlier reconciliation states that mobile favorite tests stub the API. | Stubbed browser behavior does not prove real-backend persistence, cross-session state, or synchronization; current-target run was not inspected. |
| BE-IMAGE-BOUNDARY-01 | US #3, #6, #21 | Sprint reports and prior reconciliation describe static/external image URLs and no inspected candidate-linked upload/canonical-image evidence. | No claim of upload completion, cloud storage publication, canonical image availability, or real-provider success. No cloud destination was accessed or changed. |
| BE-SHARE-BOUNDARY-01 | US #27 | Historical Sprint 3 test plan describes a share modal and opening a social URL. | Local UI/handoff does not prove external API execution, publication, or provider success. No provider result for the current candidate is claimed. |
| BE-WA-HISTORICAL-01 | US #34 | Earlier browser-evidence document reports desktop WhatsApp contract checks and mobile placement checks on `c6f50499` / run `35661433657`. | Historical handoff/placement only. It does not prove WhatsApp message sending, delivery/read status, success confirmation, or provider-side error handling on the target candidate. |
| BE-EMAIL-BOUNDARY-01 | US #35 | Sprint 4 plan distinguishes a console log from a Mailtrap inbox receipt; earlier reconciliation did not inspect a recipient/provider receipt. | No current-target claim of recipient delivery, timing, inbox rendering, or received content. |
| BE-US19-OPTIONAL-01 | US #19 | Original Sprint 2 PDF explicitly calls registration-confirmation email an optional challenge not considered in evaluation. | Optional/non-evaluated; no delivery claim is required for academic acceptance. |

## How to read the traceability matrix

See `close-user-story-gaps-traceability.md` for all official US #1–#35, including the priority from each Sprint PDF. Evidence follow-up is grouped by the four `product.md` demo journeys, then sorted within each journey by official priority `Alta` → `Media` → `Baja` and ascending US number. Journey grouping is editorial; it does not change source priority or acceptance scope. The matrix’s “documented test/code mapping” points to historical Sprint test plans/reports and likely implementation surfaces, not a current-run result. Each evaluated story remains **not verified** unless artifacts establish its official criteria against the target SHA; an evidence gap does not establish an implementation omission.

The original Sprint PDFs are the requirement authority. `product.md` describes the academic demo scope and its broader guest flow includes favorites (#24–#25), though favorites are adjacent to its explicit four-item Definition of Done. US #19 remains optional/non-evaluated and has no normal priority. US #36–#43 are later increments, outside the official 35-story matrix.

## Evidence needed to upgrade a criterion

For each candidate acceptance criterion, retain a reproducible result tied to the exact candidate SHA: test/spec name, executed job and outcome, and (for browser claims) the relevant trace/screenshot/assertion or equivalent artifact. For external-provider claims, retain direct provider/recipient evidence. If these artifacts are unavailable, leave the criterion unverified; do not infer it from a green job, implementation presence, a historical test-plan label, or a provider handoff URL.

This document records the evidence boundary only. It does not authorize provider calls, repository delivery, commit, push, pull request, merge, or release.
