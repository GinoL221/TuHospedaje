---
title: "Plan y Reporte de Pruebas de Software — Sprint 3"
subtitle: "TuHospedaje — Búsqueda, Disponibilidad, Favoritos y Contenido"
author: "Equipo de Desarrollo"
date: "Mayo 2026"
pdf_options:
  format: a4
  margin:
    top: 25mm
    bottom: 25mm
    left: 20mm
    right: 20mm
  displayHeaderFooter: true
  headerTemplate: |
    <div style="font-size: 9pt; width: 100%; text-align: right; padding-right: 20mm; color: #666;">
      TuHospedaje — Documentación Técnica Oficial
    </div>
  footerTemplate: |
    <div style="font-size: 9pt; width: 100%; display: flex; justify-content: space-between; padding: 0 20mm; color: #666;">
      <div>Sprint 3 — Mayo 2026</div>
      <div>Página <span class="pageNumber"></span> de <span class="totalPages"></span></div>
    </div>
---

<style>
.page-break { page-break-before: always; }
table { width: 100%; } table, tr { page-break-inside: avoid; }
h1, h2, h3, h4 { page-break-after: avoid; }
</style>

# PLAN Y REPORTE DE PRUEBAS DE SOFTWARE — SPRINT 3

**Foco del Incremento:** Búsqueda por ciudad y fechas, Disponibilidad y reservas, Favoritos, Políticas, Reseñas, Compartir en redes
**Enfoque de Testing:** Pruebas de API (Postman + automatizadas con MockMvc/Testcontainers), Verificación de UI Manual

## 1. Matriz Detallada de Casos de Prueba (Test Cases)

### TC-22: Módulo de Búsqueda (US #22)

* **Historias de Usuario Asociadas:** US #22 (Realizar búsqueda)
* **Precondiciones:** Servidor backend activo. Al menos 3 alojamientos en BD en diferentes ciudades.
* **Tipos de Verificación:** API Rest, Test de Integración Automatizado (JUnit 5 + MockMvc + Testcontainers).

| Paso | Acción / Estímulo de Prueba | Resultado Esperado (Criterio de Aceptación) | Estado |
|------|----------------------------|---------------------------------------------|--------|
| **1** | `GET /api/lodgings/search?city=buenos` (público) | HTTP 200. Lista filtrada por ciudad (case-insensitive) | ✔ Pasa |
| **2** | `GET /api/lodgings/search` sin parámetros | HTTP 200. Todos los alojamientos | ✔ Pasa |
| **3** | `GET /api/lodgings/search?guests=4` | HTTP 200. Solo alojamientos con maxGuests >= 4 | ✔ Pasa |
| **4** | `GET /api/lodgings/search?minPrice=100&maxPrice=200` | HTTP 200. Solo alojamientos en rango de precio | ✔ Pasa |
| **5** | `GET /api/lodgings/search?checkIn=2026-07-01&checkOut=2026-07-05` | HTTP 200. Excluye alojamientos con reservas solapadas | ✔ Pasa |
| **6** | `GET /api/lodgings/cities?q=bu` (público) | HTTP 200. Lista de ciudades que coinciden | ✔ Pasa |
| **7** | `GET /api/lodgings/cities?q=` (vacío) | HTTP 200. Todas las ciudades | ✔ Pasa |
| **8** | Búsqueda sin resultados | HTTP 200. Array vacío `[]` | ✔ Pasa |
| **9** | `GET /api/lodgings/search?checkIn=invalido` | HTTP 400 Bad Request | ✔ Pasa |

<div style="page-break-before: always;"></div>

### TC-23: Módulo de Disponibilidad y Reservas (US #23)

* **Historias de Usuario Asociadas:** US #23 (Visualizar disponibilidad)
* **Precondiciones:** Alojamiento existente en BD. Usuario autenticado con JWT válido.
* **Tipos de Verificación:** API Rest, Test de Integración Automatizado.

| Paso | Acción / Estímulo de Prueba | Resultado Esperado (Criterio de Aceptación) | Estado |
|------|----------------------------|---------------------------------------------|--------|
| **1** | `GET /api/lodgings/{id}/availability?checkIn=...&checkOut=...` (público) | HTTP 200. `{ available: true/false }` | ✔ Pasa |
| **2** | `POST /api/reservations` con datos válidos y token | HTTP 201. Reserva creada con status CONFIRMED | ✔ Pasa |
| **3** | `POST /api/reservations` en fechas ya ocupadas | HTTP 409 Conflict | ✔ Pasa |
| **4** | `POST /api/reservations` con checkOut anterior a checkIn | HTTP 400 Validation Error | ✔ Pasa |
| **5** | `POST /api/reservations` con fecha pasada | HTTP 400 Validation Error | ✔ Pasa |
| **6** | `POST /api/reservations` sin token JWT | HTTP 401 Unauthorized o 403 Forbidden | ✔ Pasa |
| **7** | `GET /api/reservations/{id}` con token válido | HTTP 200. Detalle de reserva | ✔ Pasa |
| **8** | `GET /api/reservations/{id}` con ID inexistente | HTTP 404 Not Found | ✔ Pasa |
| **9** | Dos reservas simultáneas mismo alojamiento/fechas | Una HTTP 201, la otra HTTP 409 (OptimisticLock) | ✔ Pasa |

### TC-24/25: Módulo de Favoritos (US #24, #25)

* **Historias de Usuario Asociadas:** US #24 (Marcar favorito), US #25 (Listar favoritos)
* **Precondiciones:** Usuario autenticado. Alojamiento existente.
* **Tipos de Verificación:** API Rest, UI Manual.

| Paso | Acción / Estímulo de Prueba | Resultado Esperado (Criterio de Aceptación) | Estado |
|------|----------------------------|---------------------------------------------|--------|
| **1** | `POST /api/favorites/{lodgingId}` con token | HTTP 200. Favorito agregado | ✔ Pasa |
| **2** | `GET /api/favorites` con token | HTTP 200. Lista de favoritos del usuario | ✔ Pasa |
| **3** | `DELETE /api/favorites/{lodgingId}` con token | HTTP 200. Favorito eliminado | ✔ Pasa |
| **4** | `POST /api/favorites/{lodgingId}` sin token | HTTP 401/403 | ✔ Pasa |
| **5** | Corazón visible en ProductCard (autenticado) | Se muestra icono ♥ | ✔ Pasa |
| **6** | Corazón NO visible en ProductCard (anónimo) | No se muestra icono | ✔ Pasa |
| **7** | Click corazón agrega/quita favorito | Toggle visual + llamada API | ✔ Pasa |

### TC-26: Módulo de Políticas (US #26)

* **Historias de Usuario Asociadas:** US #26 (Ver bloque de políticas)
* **Precondiciones:** Políticas creadas en BD. Alojamiento con políticas asociadas.
* **Tipos de Verificación:** API Rest, UI Manual.

| Paso | Acción / Estímulo de Prueba | Resultado Esperado (Criterio de Aceptación) | Estado |
|------|----------------------------|---------------------------------------------|--------|
| **1** | `GET /api/policies` (público) | HTTP 200. Lista de políticas | ✔ Pasa |
| **2** | `POST /api/policies` con token ADMIN | HTTP 201. Política creada | ✔ Pasa |
| **3** | `POST /api/policies` sin token ADMIN | HTTP 403 Forbidden | ✔ Pasa |
| **4** | Sección de políticas en ProductDetail | Título subrayado, columnas con ícono + título + descripción | ✔ Pasa |

### TC-27: Módulo de Compartir (US #27)

* **Historias de Usuario Asociadas:** US #27 (Compartir en redes sociales)
* **Precondiciones:** Alojamiento visible en ProductDetail.
* **Tipos de Verificación:** UI Manual.

| Paso | Acción / Estímulo de Prueba | Resultado Esperado (Criterio de Aceptación) | Estado |
|------|----------------------------|---------------------------------------------|--------|
| **1** | Click botón "Compartir" en ProductDetail | Pop-up con opciones Facebook, Twitter, WhatsApp | ✔ Pasa |
| **2** | Pop-up muestra imagen + descripción + enlace | Contenido visible y correcto | ✔ Pasa |
| **3** | Click en red social | Abre nueva pestaña con URL de compartir | ✔ Pasa |

<div style="page-break-before: always;"></div>

### TC-28: Módulo de Reseñas (US #28)

* **Historias de Usuario Asociadas:** US #28 (Puntuar producto)
* **Precondiciones:** Alojamiento existente. Usuario autenticado.
* **Tipos de Verificación:** API Rest, UI Manual.

| Paso | Acción / Estímulo de Prueba | Resultado Esperado (Criterio de Aceptación) | Estado |
|------|----------------------------|---------------------------------------------|--------|
| **1** | `POST /api/ratings` con token y datos válidos | HTTP 201. Reseña creada | ✔ Pasa |
| **2** | `POST /api/ratings` sin token | HTTP 401/403 | ✔ Pasa |
| **3** | `GET /api/ratings/lodging/{id}` (público) | HTTP 200. Lista de reseñas con promedio | ✔ Pasa |
| **4** | Formulario de reseña visible solo para autenticados | Selector de estrellas + textarea | ✔ Pasa |
| **5** | Promedio de estrellas se actualiza al agregar reseña | Cálculo dinámico correcto | ✔ Pasa |
| **6** | Lista de reseñas muestra nombre, fecha, estrellas, comentario | Datos visibles y formateados | ✔ Pasa |

<div style="page-break-before: always;"></div>

### TC-29: Módulo de Eliminar Categoría (US #29)

* **Historias de Usuario Asociadas:** US #29 (Eliminar categoría)
* **Precondiciones:** Categorías existentes en BD. Usuario ADMIN autenticado.
* **Tipos de Verificación:** UI Manual.

| Paso | Acción / Estímulo de Prueba | Resultado Esperado (Criterio de Aceptación) | Estado |
|------|----------------------------|---------------------------------------------|--------|
| **1** | Click "Eliminar" en categoría | ConfirmDialog con mensaje descriptivo | ✔ Pasa |
| **2** | Click "Confirmar" en diálogo | Categoría eliminada, tabla refrescada | ✔ Pasa |
| **3** | Click "Cancelar" en diálogo | Diálogo cerrado, categoría intacta | ✔ Pasa |

## 2. Resumen de Ejecución

| Tipo de Prueba | Cantidad | Estado |
|---------------|----------|--------|
| Tests Automatizados Backend (JUnit 5 + MockMvc) | 120 tests | ✔ Todos pasan |
| Casos de Prueba Funcionales (Plan) | 40 escenarios | ✔ 40/40 verificados |

## 3. Cobertura por Historia de Usuario

| User Story | Cantidad TC | Estado |
|-----------|-------------|--------|
| US #22 — Búsqueda | 9 TC | ✔ Automatizado |
| US #23 — Disponibilidad | 9 TC | ✔ Automatizado |
| US #24/#25 — Favoritos | 7 TC | ✔ Automatizado + Manual |
| US #26 — Políticas | 4 TC | ✔ Automatizado + Manual |
| US #27 — Compartir | 3 TC | ✔ Manual |
| US #28 — Reseñas | 6 TC | ✔ Automatizado + Manual |
| US #29 — Eliminar categoría | 3 TC | ✔ Manual |

## 4. Herramientas Utilizadas

| Herramienta | Propósito |
|------------|-----------|
| JUnit 5 + Mockito | Tests unitarios de servicios |
| MockMvc + Testcontainers | Tests de integración con MariaDB efímera |
| Postman | Pruebas manuales de API |
| Navegador (Chrome) | Verificación de UI |
| Swagger UI | Documentación y exploración de endpoints |

## 5. Defectos Encontrados

| ID | Descripción | Severidad | Estado |
|----|------------|-----------|--------|
| — | Ningún defecto crítico encontrado en Sprint 3 | — | — |
