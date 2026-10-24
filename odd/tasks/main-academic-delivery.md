# Main academic delivery

Objective: leave `main` as the academic deliverable, documented with the documents the user stories require, the corresponding tests, reproducible and correctable.
Branch: `docs/main-academic-delivery`. Delivery: `ask-on-risk`; forecast exceeds 400 authored lines; `chain_strategy=feature-branch-chain`.
Runner: no single new runner. Existing commands stay in the sprint test plans and README until a story is executed.

## Specs

- S1: "dejamos main como entrega"
- S2: "lo dejamos bien documentado, con los documentos que piden las notas de usuario, los test correspondientes, reproducible y corregible"
- Assumption, not a user quote: sprint branches are not rebuilt. The user accepted `main` as the delivery after that recommendation.

## Tasks

- T1 S1-S2 inline done: inventory the documents already on `main` and record the evidence gap. No commit.
- T2 S2 inline done: write the current-`main` delivery index without promoting unverified stories. Commit `5002aa022df6fc0ca6bd3049e97c9eb772b4b433`.
- T3 S2 inline blocked: bind the corresponding tests to current `main`, one story group at a time. Waits for the user to name the first group and to authorize any write to the preserved local database.
- T4 S1-S2 inline pending: update the traceability pin only after T3 evidence exists. Do not mark a story verified from an old SHA.

## Log

- L1: "bien, hacemos eso entonces, dejamos main como entrega, pero lo dejamos bien documentado, con los documentos que piden las notas de usuario, los test correspondientes, reproducible y corregible"
- L2: Inventory on 2026-10-24. `origin/main` is `a430c73938663df2d7c72eb23b9e730d25c3a48a`. `docs/entregables/` has the project definition and sprint 1–4 report and test-plan PDFs. Markdown reports and test plans exist under `docs/markdown/sprint-1/` through `sprint-4/`. `product.md` defines the demo journeys. The vault standard `01-Conocimiento/Conceptos/Estandar-Documentacion-Tecnica.md` requires a user-story table with UI component, endpoint, and acceptance criterion, explicit known limitations, and Mermaid diagrams exported to SVG. The official `Sprint 1.pdf` through `Sprint 4.pdf` named by `docs/markdown/compliance/close-user-story-gaps-traceability.md` are not in the repository. That traceability file is pinned to `1572f121e91a29257aa618618873d6b560df863a` and says no story is verified on that candidate. It is not proof for current `main`.
- L3: Delivery strategy chosen: `chain_strategy=feature-branch-chain`. T2 commit `5002aa022df6fc0ca6bd3049e97c9eb772b4b433`, `docs: index the current main academic delivery`. No story was marked verified. Push and PR were not requested.
- L4: The user chose registration, login, and roles as the first group and then authorized Testcontainers. Frontend Vitest command `npx vitest run src/pages/RegisterPage.test.jsx src/pages/LoginPage.test.jsx src/components/Header/Header.test.jsx src/pages/Admin/AdminUsers.test.jsx` returned 4 files and 40 tests passed. Backend command `cd backend && ./mvnw -B -Dtest=AuthControllerIntegrationTest,UserControllerIntegrationTest test` returned BUILD SUCCESS: AuthControllerIntegrationTest 28, UserControllerIntegrationTest 11, total 39, 0 failures. JDBC URLs were `jdbc:mariadb://localhost:32769/test` and `jdbc:mariadb://localhost:32770/test` on disposable `mariadb:10.11` containers. The preserved local database was not used. Browser steps in TC-13, TC-14, and TC-16 were not run. No story was marked verified.
