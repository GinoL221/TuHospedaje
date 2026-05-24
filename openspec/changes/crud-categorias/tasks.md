---
id: crud-categorias-tasks
title: Tasks — CRUD de Categorías
status: draft
date: 2026-05-21
---

# Tasks: CRUD de Categorías

## Review Workload Forecast

| Field | Value |
|---|---|
| Estimated changed lines | 700–980 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR1 Backend Category + tests → PR2 Lodging+Security → PR3 Admin frontend |
| Delivery strategy | ask-on-risk |
| Chain strategy | pending |

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: pending
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|---|---|---|---|
| 1 | CRUD Category backend completo | PR 1 | Base para integración, incluye T1-T6 |
| 2 | Integración Lodging + seguridad | PR 2 | Depende PR1, incluye T7-T9 |
| 3 | Admin categorías + selector lodging | PR 3 | Depende PR2, incluye T10-T12 |

## Fase 1: Backend Category CRUD (T1-T6)

- [x] 1.1 **T1 Crear `Category` entity** — Deps: -; Files: `backend/src/main/java/com/tuhospedaje/entity/Category.java`; Effort: 1f/~45L; AC: `id,name,description` + constraints y tabla `categories`; Pri: P1; Riesgo: constraint unique mal definida.
- [x] 1.2 **T2 Crear `CategoryDTO`** — Deps: T1; Files: `backend/src/main/java/com/tuhospedaje/dto/CategoryDTO.java`; Effort: 1f/~55L; AC: mapeo manual `toEntity/fromEntity` consistente con Lodging; Pri: P1; Riesgo: mapear nulos incorrectamente.
- [x] 1.3 **T3 Crear `CategoryRepository`** — Deps: T1; Files: `backend/src/main/java/com/tuhospedaje/repository/CategoryRepository.java`; Effort: 1f/~20L; AC: `JpaRepository` + lookup/exists por nombre; Pri: P1; Riesgo: método de duplicado no case-insensitive.
- [x] 1.4 **T4 Crear `ICategoryService` + `CategoryServiceImpl`** — Deps: T2,T3; Files: `backend/src/main/java/com/tuhospedaje/service/ICategoryService.java`, `backend/src/main/java/com/tuhospedaje/service/impl/CategoryServiceImpl.java`; Effort: 2f/~130L; AC: create/list/get/update/delete con 409 duplicado y 404 not found; Pri: P1; Riesgo: errores no pasan por `GlobalExceptionHandler`.
- [x] 1.5 **T5 Crear `CategoryController`** — Deps: T4; Files: `backend/src/main/java/com/tuhospedaje/controller/CategoryController.java`; Effort: 1f/~90L; AC: endpoints CRUD con `201/200/204/404` y `@PreAuthorize` en escritura; Pri: P1; Riesgo: contrato DELETE responde body en vez de 204.
- [x] 1.6 **T6 Tests unitarios + integración Category** — Deps: T5; Files: `backend/src/test/java/com/tuhospedaje/category/CategoryServiceImplTest.java`, `backend/src/test/java/com/tuhospedaje/category/CategoryControllerIntegrationTest.java`; Effort: 2f/~170L; AC: cubre escenarios spec (create válido/vacío/duplicado, list público, update, delete, auth write); Pri: P1; Riesgo: setup de seguridad rompe tests de integración.

## Fase 2: Integrar Category en Lodging (T7-T9)

- [x] 2.1 **T7 Relación en `Lodging`** — Deps: T1; Files: `backend/src/main/java/com/tuhospedaje/entity/Lodging.java`; Effort: 1f/~25L; AC: `@ManyToOne(fetch=LAZY)` + `@JoinColumn(name="category_id", nullable=true)`; Pri: P1; Riesgo: lazy loading fuera de contexto transaccional.
- [x] 2.2 **T8 Extender `LodgingDTO` + mapear en service** — Deps: T7,T4; Files: `backend/src/main/java/com/tuhospedaje/dto/LodgingDTO.java`, `backend/src/main/java/com/tuhospedaje/service/impl/LodgingServiceImpl.java`; Effort: 2f/~120L; AC: `categoryId/categoryName` en create/update/get/list y 404 si `categoryId` no existe; Pri: P1; Riesgo: regresión en create/update de lodging existente.
- [x] 2.3 **T9 Ajustar `SecurityConfig` GET público categorías** — Deps: T5; Files: `backend/src/main/java/com/tuhospedaje/configuration/SecurityConfig.java`; Effort: 1f/~15L; AC: `GET /api/categories/**` público; POST/PUT/DELETE autenticados; Pri: P1; Riesgo: abrir rutas extra por matcher demasiado amplio.

## Fase 3: Frontend Admin (T10-T12)

- [ ] 3.1 **T10 Navegación interna en `Admin.jsx`** — Deps: T6; Files: `frontend/src/pages/Admin/Admin.jsx`, `frontend/src/pages/Admin/Admin.css`; Effort: 2f/~90L; AC: tabs “Alojamientos | Categorías” con estado activo y sin romper paginación actual; Pri: P2; Riesgo: estado compartido causa renders inconsistentes.
- [ ] 3.2 **T11 CRUD categorías en Admin** — Deps: T10; Files: `frontend/src/pages/Admin/Admin.jsx`, `frontend/src/pages/Admin/Admin.css`, `frontend/src/services/api.js`; Effort: 3f/~180L; AC: tabla categorías + modal crear/editar + delete; `api.js` tolera `204 No Content`; Pri: P1; Riesgo: parsing JSON en 204 dispara error de UI.
- [ ] 3.3 **T12 Selector de categoría en modal lodging** — Deps: T8,T11; Files: `frontend/src/pages/Admin/Admin.jsx`; Effort: 1f/~60L; AC: `<select>` carga categorías, envía `categoryId` (o null) y persiste en alta de lodging; Pri: P2; Riesgo: desincronización entre catálogo y formulario.

## Rollback plan

- **Fase 1**: revertir T1-T6 (controllers/services/repos/tests de category); mantener tabla `categories` vacía sin impacto.
- **Fase 2**: revertir T7-T9; `category_id` nullable puede quedar sin romper lodgings actuales.
- **Fase 3**: revertir T10-T12 en `Admin.jsx/Admin.css/api.js`; vuelve panel original de alojamientos.
