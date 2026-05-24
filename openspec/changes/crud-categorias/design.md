---
id: crud-categorias-design
title: Diseño Técnico — CRUD de Categorías
status: draft
date: 2026-05-21
version: "1.0"
---

## 1. Arquitectura

El cambio sigue el patrón existente de `Lodging`: `Entity → Repository → Service (interface + impl) → Controller → DTO`. `CategoryController` expondrá CRUD REST, `CategoryServiceImpl` concentrará validaciones y `CategoryRepository` resolverá acceso a datos. El mapping será manual en `CategoryDTO`, igual que hoy en `LodgingDTO`, y los errores funcionales (`duplicado`, `no encontrado`) seguirán pasando por `GlobalExceptionHandler`.

En frontend, `Admin.jsx` se amplía con navegación interna por secciones, manteniendo el patrón actual de tabla + modal. Se agrega una vista de categorías con un único modal reutilizado para crear/editar, y el modal de alojamientos suma un `<select>` para elegir categoría. Se reutilizan `get/post/put/del` de `frontend/src/services/api.js`; como el contrato de borrado de categorías será `204 No Content`, el helper debe tolerar respuestas vacías.

## 2. Decisiones técnicas

| Decisión | Opción elegida | Alternativa | Motivo |
|----------|---------------|-------------|--------|
| Mapping DTO | `toEntity/fromEntity` manual | MapStruct | Consistencia con `LodgingDTO`, sin dependencias nuevas |
| Modal CRUD | Un solo modal crear/editar | Modal separado | Menos código duplicado y mismo patrón de Admin actual |
| Selector categoría | `<select>` en formulario | Typeahead | Simplicidad, pocas categorías y sin librería extra |
| Relación Lodging→Category | `@ManyToOne(fetch = LAZY)` | `EAGER` | Evita cargas innecesarias; solo se resuelve cuando el DTO lo necesita |
| Autorización de escritura | `@PreAuthorize("hasRole('ADMIN')")` + GET público en `SecurityConfig` | Solo reglas por URL | Cumple la convención del proyecto y separa lectura pública de escritura admin |
| DELETE categories | `204 No Content` y ajuste en `api.js` | Responder texto/json | Respeta el contrato pedido sin romper el helper `fetch` existente |

## 3. Contratos API

| Método | Endpoint | Request | Response | Auth |
|--------|----------|---------|----------|------|
| POST | `/api/categories` | `{ name, description? }` | `CategoryDTO` (201) | Admin |
| GET | `/api/categories` | - | `CategoryDTO[]` (200) | Público |
| GET | `/api/categories/{id}` | - | `CategoryDTO` (200) / 404 | Público |
| PUT | `/api/categories/{id}` | `{ name, description? }` | `CategoryDTO` (200) / 404 | Admin |
| DELETE | `/api/categories/{id}` | - | `204` / `404` | Admin |

## 4. Modelo de datos

```json
Category {
  id: Long (PK, auto)
  name: String (unique, not null)
  description: String (nullable)
}

Lodging (modificado):
  + category_id: Long (FK → categories.id, nullable)
```

La columna `category_id` queda nullable para no romper alojamientos existentes. `LodgingDTO` expone `categoryId` para escritura y `categoryName` para lectura en UI sin obligar al frontend a resolver joins.

## 5. Cambios en archivos

**Crear:**
- `backend/src/main/java/com/tuhospedaje/entity/Category.java`
- `backend/src/main/java/com/tuhospedaje/dto/CategoryDTO.java`
- `backend/src/main/java/com/tuhospedaje/repository/CategoryRepository.java`
- `backend/src/main/java/com/tuhospedaje/service/ICategoryService.java`
- `backend/src/main/java/com/tuhospedaje/service/impl/CategoryServiceImpl.java`
- `backend/src/main/java/com/tuhospedaje/controller/CategoryController.java`

**Modificar:**
- `backend/src/main/java/com/tuhospedaje/entity/Lodging.java` (+ `@ManyToOne`, `@JoinColumn(name = "category_id")`)
- `backend/src/main/java/com/tuhospedaje/dto/LodgingDTO.java` (+ `categoryId`, `categoryName`)
- `backend/src/main/java/com/tuhospedaje/service/impl/LodgingServiceImpl.java` (resolver categoría en save/update y devolver nombre en response)
- `backend/src/main/java/com/tuhospedaje/configuration/SecurityConfig.java` (+ `GET /api/categories/**` público y habilitación de method security)
- `frontend/src/pages/Admin/Admin.jsx` (+ navegación interna, tabla/modal de categorías, selector en lodging)
- `frontend/src/pages/Admin/Admin.css` (+ estilos de nueva sección y estados de menú)
- `frontend/src/services/api.js` (soporte para `204 No Content`)

## 6. Estrategia de testing

- **Unitarios `CategoryServiceImpl`**: crear categoría válida, rechazar nombre duplicado y lanzar `ResourceNotFoundException` en update/delete/find por id inexistente.
- **Integración `CategoryController`**: CRUD completo con `MockMvc`, validando `201/200/204/404`, lectura pública de GET y restricción admin en POST/PUT/DELETE.
- **Regresión backend de lodging**: cubrir save/update con `categoryId` válido y nulo para asegurar compatibilidad hacia atrás.

## 7. Orden de implementación

1. Backend Category (`entity → DTO → repository → service → controller`)
2. Conectar Lodging con Category (`entity`, DTO, service)
3. `SecurityConfig`
4. Frontend admin (sección categorías + selector en lodging)
