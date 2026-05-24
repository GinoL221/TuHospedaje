---
id: crud-categorias
title: CRUD de Categorías
status: proposal
date: 2026-05-21
participants:
  - role: developer
    name: Gino
description:
  short: CRUD completo de categorías con integración en alojamientos
  long: >
    Implementar el módulo de categorías para alojamientos, permitiendo
    crear, editar, eliminar y listar categorías desde el panel admin,
    y asignar categorías a alojamientos en creación/edición.
tags:
  - sprint-2
  - crud
  - backend
  - frontend
change_type: feature
---

# Proposal: CRUD de Categorías

## Intent

Las categorías son un requisito fundamental para organizar y clasificar alojamientos (user stories #12, #20, #21 del Sprint 2). Sin este módulo, los usuarios no pueden filtrar ni identificar el tipo de alojamiento, y los admins no tienen forma de gestionar la taxonomía del sistema. Este cambio establece la base para futuras funcionalidades de búsqueda y filtrado.

## Scope

### In Scope
- **Backend**: Entity `Category`, `CategoryRepository`, `ICategoryService` + `CategoryServiceImpl`, `CategoryController`, DTOs
- **Integración Lodging**: `Lodging` con `@ManyToOne Category`, `LodgingDTO` con `categoryId`, mapeo en `LodgingServiceImpl`
- **Frontend**: Sección de categorías en `Admin.jsx` (CRUD con modal), selector de categoría en modal de lodging
- **Security**: `GET /api/categories/**` público

### Out of Scope
- Filtrar alojamientos por categoría (otra story)
- Sección de categorías en el home público
- Imagen o ícono de categoría
- Edición de categoría en el modal de lodging (solo creación)

## Capabilities

### New Capabilities
- `category-management`: CRUD de categorías (crear, listar, editar, eliminar), visibilidad pública de listado, asignación de categoría a alojamientos

### Modified Capabilities
- `user-auth`: Ruta `/api/categories/**` se agrega como pública en la tabla de protección de rutas

## Approach

**Backend**: Patrón idéntico al de Lodging — `Category` entity con `@Table(name="categories")`, `CategoryDTO` con métodos `toEntity()`/`fromEntity()`, service interface + impl, controller REST con constructor injection. Relación `@ManyToOne` desde `Lodging` hacia `Category` con `FetchType.LAZY`. `category_id` nullable para alojamientos existentes.

**Frontend**: Sección "Categorías" en `Admin.jsx` con tabla + modal reutilizado para crear/editar/eliminar. Selector `<select>` en el modal de lodging para asignar categoría. `api.js` ya soporta GET/POST/PUT/DELETE sin cambios.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `backend/.../entity/Category.java` | New | Entity con id, name, description |
| `backend/.../dto/CategoryDTO.java` | New | DTO con toEntity/fromEntity |
| `backend/.../repository/CategoryRepository.java` | New | JpaRepository + findByNombre |
| `backend/.../service/ICategoryService.java` | New | Interface CRUD |
| `backend/.../service/impl/CategoryServiceImpl.java` | New | Implementación |
| `backend/.../controller/CategoryController.java` | New | REST controller |
| `backend/.../entity/Lodging.java` | Modified | + `@ManyToOne Category category` |
| `backend/.../dto/LodgingDTO.java` | Modified | + `categoryId`, mapeo en toEntity/fromEntity |
| `backend/.../service/impl/LodgingServiceImpl.java` | Modified | Resolver category por ID en save/update |
| `backend/.../configuration/SecurityConfig.java` | Modified | + `GET /api/categories/**` público |
| `frontend/src/pages/Admin/Admin.jsx` | Modified | + sección categorías, selector en modal lodging |
| `frontend/src/pages/Admin/Admin.css` | Modified | Estilos para sección categorías |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| `category_id` nullable en tabla existente | Low | JPA crea columna nullable por defecto. Alojamientos sin categoría = null seguro |
| Sin tests previos de Lodging | Medium | Seguir patrón Lodging exacto. Tests se agregan en fase verify |
| Admin.jsx crece demasiado | Medium | Mantener secciones separadas con estados independientes. Si supera 400 líneas, evaluar extracción de componente |
| Circular dependency DTO ↔ Entity | Low | `CategoryDTO` importa `Category` (unidireccional). `LodgingDTO` no importa `Category` directamente, usa `categoryId` |

## Rollback Plan

1. Revertir commit(s) del change en la rama `sprint-2`
2. La columna `category_id` en `lodgings` puede quedar — es nullable y no afecta funcionalidad
3. La tabla `categories` puede quedar vacía — no impacta endpoints existentes
4. Frontend: revertir `Admin.jsx` y `Admin.css` a estado pre-categorías
5. `SecurityConfig.java`: remover ruta `/api/categories/**`

## Dependencies

- Ninguna dependencia externa. Auth flow ya implementado en Sprint 2.
- MariaDB corriendo con schema actualizado (tablas `categories`, columna `category_id` en `lodgings`)

## Success Criteria

- [ ] Admin crea categoría con nombre y descripción
- [ ] Admin edita categoría existente
- [ ] Admin elimina categoría (sin alojamientos asociados)
- [ ] Admin lista todas las categorías en el panel
- [ ] Admin asigna categoría a alojamiento en creación
- [ ] GET `/api/categories` retorna lista sin autenticación
- [ ] Alojamiento sin categoría funciona correctamente (category = null)
- [ ] No se rompen endpoints existentes de lodgings
