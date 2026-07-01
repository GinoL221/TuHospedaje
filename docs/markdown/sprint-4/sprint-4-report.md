---
title: "Bitácora de Ejecución y Cierre — Sprint 4"
subtitle: "TuHospedaje — Reservas, Historial, WhatsApp, Email, Panel de Administración y Refactor de Arquitectura"
author: "Equipo de Desarrollo"
date: "Junio-Julio 2026"
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
      <div>Sprint 4 — Junio-Julio 2026</div>
      <div>Página <span class="pageNumber"></span> de <span class="totalPages"></span></div>
    </div>
---

<style>
.page-break { page-break-before: always; }
table { width: 100%; } table, tr { page-break-inside: avoid; }
h1, h2, h3, h4 { page-break-after: avoid; }
</style>

# BITÁCORA DE EJECUCIÓN Y CIERRE — SPRINT 4

**Foco del Incremento:** Reservas, Historial y Comunicación (Incremento 1) · Tablas Uniformes y Dashboard de Reservas (Incremento 2) · Refactor de Arquitectura y Mejoras (Incremento 3)
**Stack Tecnológico:** Java 17 / Spring Boot 3.5 / Spring Security 6 / JavaMailSender / MariaDB / React 19 / Vite / Testcontainers / SpringDoc OpenAPI

Este documento consolida los tres incrementos ejecutados sobre la rama `sprint-4` para la entrega final: el alcance original del sprint (reservas, WhatsApp, email), la consolidación posterior del panel de administración (previamente documentada como "Sprint 4.5"), y el refactor de arquitectura de cierre. Se presentan de forma unificada, no como reportes separados, para reflejar el estado final del incremento.

## 1. Resumen del Incremento (Scope)

### 1.1. Incremento 1 — Reservas, Historial y Comunicación

El Sprint 4 completó la funcionalidad central de reservas de la plataforma. Se reemplazó el modal de reserva provisional (Sprint 3) por un flujo de página dedicada en dos columnas (`/booking/:lodgingId`), que presenta el resumen completo del alojamiento junto al formulario de reserva. Al confirmar, el usuario es redirigido a una pantalla de confirmación con los detalles de la estadía.

Se implementó el historial personal de reservas (`/my-reservations`) con ordenamiento por fecha de entrada descendente, protegido por el nuevo guard `RequireAuth`. Se incorporó un botón flotante de WhatsApp configurable por variable de entorno, y se desarrolló un servicio de email SMTP real (`SmtpEmailServiceImpl`) que envía confirmaciones HTML al huésped mediante Mailtrap, activable de forma independiente al `ConsoleEmailServiceImpl` ya existente.

En el backend, se corrigió el endpoint de disponibilidad para retornar los rangos ocupados (`occupiedRanges`) sin necesidad de recibir fechas, permitiendo el bloqueo visual del calendario al cargar la página. Se agregó validación de reserva confirmada antes de permitir puntuaciones (`RatingServiceImpl`), y se añadieron 5 nuevos tests de integración con Testcontainers.

Como mejora complementaria al panel de administración, se extendió `LodgingFormModal` con soporte de edición (prop `lodging` opcional + `PUT`) y se incorporaron los botones "Editar" por fila en la tabla de alojamientos.

Adicionalmente, como agregado por iniciativa del equipo fuera del alcance original del sprint, se incorporó una suite de pruebas end-to-end con Playwright (carpeta `e2e/`), que valida los flujos críticos de la aplicación en Chromium y Firefox e introduce pruebas de regresión visual con capturas de referencia.

### 1.2. Incremento 2 — Tablas Uniformes y Dashboard de Reservas

Este incremento consolidó la experiencia de usuario en el Panel de Administración mediante dos ejes principales: la unificación de tablas y la visualización de reservas recientes.

Se estandarizó la lógica de presentación de datos mediante el hook personalizado `useTableData` y los componentes reutilizables `SortableTh` (encabezados ordenables) y `Pagination` (control de páginas), integrados en las vistas de Categorías, Características, Políticas y Usuarios. Para mantener la consistencia del panel, se migró la tabla de alojamientos (`AdminLodgings`) de paginación del servidor a paginación del cliente (`GET /api/lodgings` plano), agregando además la columna de descripción. El componente de paginación unificado también se integró en `SearchResults` en este momento (antes de que el Incremento 3 lo hiciera server-driven nuevamente para ese caso puntual — ver 1.3).

Se implementó el soporte para visualizar las reservas recientes en el Dashboard del administrador: el endpoint `GET /api/reservations` (exclusivo ADMIN) retorna todas las reservas ordenadas de forma descendente por ID, y la UI del Dashboard se enriqueció con una tarjeta de estadísticas de "Reservas" y una sección "Últimas reservas" con las primeras 4 transacciones.

### 1.3. Incremento 3 — Refactor de Arquitectura y Mejoras

Cierre de deuda técnica identificada durante el desarrollo del panel de administración, ejecutado mediante un ciclo SDD completo (propuesta → especificación → diseño → tareas → implementación) y entregado en 6 pull requests encadenadas.

**Backend:** se reemplazaron los campos `Map<String, Object>` de `features`/`policies` en `LodgingDTO` por DTOs tipados (`FeatureSummaryDTO`, `PolicySummaryDTO`). El endpoint `GET /api/lodgings/search` pasó de devolver un array plano a soportar paginación real (`page`/`size`) y filtrado por múltiples categorías directamente en base de datos (expresión `IN` sobre la `Specification` JPA existente, reemplazando el filtrado en memoria que hacía el frontend) — la respuesta ahora usa el mismo formato paginado que ya utilizaba `GET /api/lodgings`. Se agregó internacionalización (`Accept-Language`) acotada a 4 de los 9 exception handlers de `GlobalExceptionHandler` (`ResourceNotFoundException`, `IllegalArgumentException`, y los nuevos handlers de validación de parámetros `ConstraintViolationException`/`HandlerMethodValidationException`), con bundles `messages.properties`/`messages_es.properties`.

**Frontend:** se creó una capa de servicios por dominio (`lodgingService.js`, `categoryService.js`, `favoriteService.js`) para desacoplar las llamadas HTTP directas de los componentes. `SearchResults.jsx` se reescribió para consumir esta capa, eliminando la paginación client-side y el filtrado en memoria de múltiples categorías en favor del nuevo contrato paginado del servidor. `RequireAdmin` se convirtió en un layout basado en `<Outlet/>` (alineado con el patrón ya usado por `RequireAuth`), y el redirect para usuarios autenticados sin rol ADMIN cambió de `/` a una nueva página `/unauthorized`.

**Nota sobre paginación:** el Incremento 2 había documentado como deuda técnica controlada "migrar [alojamientos] a paginación por base de datos únicamente si la volumetría de producción lo requiere". El Incremento 3 resuelve esto específicamente para `/api/lodgings/search` (endpoint público, donde el filtrado en memoria de categorías múltiples ya era una ineficiencia medible), **no** para las tablas del panel de administración (`AdminLodgings` y el resto), que permanecen con paginación client-side por decisión explícita del Incremento 2 — volúmenes de datos bajos/medios en un contexto exclusivamente administrativo.

## 2. Arquitectura del Sistema e Integración

### 2.1. Backend (Spring Boot + Spring Security 6)

Se expandió la arquitectura existente sin introducir nuevos módulos estructurales:

```
Controller → Service (Interface + Impl) → Repository → Entity / DTO
```

#### Matriz de Componentes Introducidos o Modificados:

| Módulo | Entidad / DTO Afectado | Cambio en Capa de Servicio | Cambio en Controller |
|--------|------------------------|---------------------------|----------------------|
| **Reservas (Inc. 1)** | `Reservation` (+`guestPhone`), `ReservationResponse`, `CreateReservationRequest` | `ReservationService` / `ReservationServiceImpl`: historial propio | `ReservationController`: `GET /api/reservations/my`, corrección de `availability` |
| **Email (Inc. 1)** | — | `SmtpEmailServiceImpl` (nueva, `@Primary`), `ConsoleEmailServiceImpl` (renombrado) | — |
| **Alojamientos (Inc. 1)** | `LodgingDTO` (+`averageRating`, +`ratingCount`) | `LodgingServiceImpl.enrichWithRatings()` | — |
| **Categorías (Inc. 1)** | `Category` (+`imageUrl`) | — | — |
| **Reseñas (Inc. 1)** | — | `RatingServiceImpl`: validación de reserva CONFIRMED previa | — |
| **Reservas — Admin (Inc. 2)** | `Reservation`, `ReservationResponse` | `ReservationServiceImpl`: todas las reservas ordenadas por ID desc | `ReservationController`: `GET /api/reservations`, `@PreAuthorize("hasRole('ADMIN')")` |
| **Alojamientos — DTOs tipados (Inc. 3)** | `LodgingDTO` (features/policies tipados), `FeatureSummaryDTO`, `PolicySummaryDTO` (nuevos) | `LodgingServiceImpl`: mapeo manual actualizado | — |
| **Alojamientos — Búsqueda (Inc. 3)** | — | `LodgingServiceImpl.search()`: `Specification` con `IN` sobre categorías, `Pageable`, retorna `Map` paginado | `LodgingController`: `/search` acepta `categories`, `page`, `size` validados (`@Validated` + `@Min`) |
| **Excepciones — i18n (Inc. 3)** | `ResourceNotFoundException` (+`errorCode`, +`args`, retrocompatible) | — | `GlobalExceptionHandler`: 4 de 9 handlers localizados vía `MessageSource`/`Locale` |

* **SmtpEmailServiceImpl con `@Primary`:** se optó por `@Primary` en lugar de `@ConditionalOnMissingBean` porque esta última anotación solo evalúa confiablemente en clases `@Configuration`. `@Primary` resuelve la ambigüedad de forma explícita y determinista.
* **Endpoint de disponibilidad corregido:** `GET /api/lodgings/{id}/availability` acepta `checkIn`/`checkOut` opcionales. Sin parámetros, devuelve todos los rangos CONFIRMED (`occupiedRanges`) para bloqueo visual del calendario.
* **Validación de reseñas:** `RatingServiceImpl.createRating()` verifica `existsByUserIdAndLodgingIdAndStatus(CONFIRMED)` antes de persistir.
* **Restricción de acceso a `/api/reservations` (Inc. 2):** `@PreAuthorize("hasRole('ADMIN')")`. Confirmado por integración: sin credenciales → 401, no-admin → 403, admin → 200 con listado ordenado por ID desc.
* **Búsqueda multi-categoría en base de datos (Inc. 3):** la `Specification` JPA existente (que ya filtraba por una única categoría con `equal`) se extendió a `root.get("category").get("id").in(categories)`, evitando múltiples queries o filtrado en memoria.
* **Validación de `page`/`size` con Bean Validation, no con excepciones ad-hoc (Inc. 3):** se evaluó usar el mensaje de la excepción como clave de traducción (`IllegalArgumentException`), pero se descartó porque ningún punto real del código (13 sitios existentes) usa ese patrón — todos lanzan texto plano en español. Se optó por `@Validated` + `@Min(message="{clave}")` en `LodgingController`, el mecanismo idiomático de Spring para localizar validaciones de parámetros. Es el primer controller del proyecto en usar `@Validated` a nivel de clase (el resto de las validaciones existentes son `@Valid` sobre `@RequestBody`, que no lo requiere).
* **Corrección de un bug de entorno real (Inc. 3):** `spring.messages.fallback-to-system-locale` (default `true` en Spring Boot) hacía que, en un host con locale del sistema operativo en español, pedir `Accept-Language: en` devolviera igualmente el mensaje en español — comportamiento no determinístico según el entorno de ejecución. Se deshabilitó explícitamente en `application.properties` (main y test).
* **Alcance acotado de la i18n (Inc. 3):** de los 9 exception handlers de `GlobalExceptionHandler`, solo 4 fueron localizados (`ResourceNotFoundException`, `IllegalArgumentException`, `ConstraintViolationException`, `HandlerMethodValidationException`). Los otros 5 (`AuthenticationException`, `MethodArgumentNotValidException`, `ObjectOptimisticLockingFailureException`, `PessimisticLockingFailureException`, `UploadException`, `DataIntegrityViolationException` y el catch-all genérico) mantienen sus mensajes en español hardcodeado — decisión explícita para no inflar el alcance del cambio (ver Sección 8).
* **Nuevas queries en repositorios:**
  - `ReservationRepository.findByUserIdOrderByCheckInDesc(Long userId)`
  - `ReservationRepository.existsByUserIdAndLodgingIdAndStatus(...)`
  - `ReservationRepository.findAllByOrderByIdDesc()` (Inc. 2)
  - `RatingRepository.countByLodgingId(Long lodgingId)`

### 2.2. Frontend (React + Vite)

```
src/
├── components/
│   ├── RequireAuth.jsx              (Inc. 1 — route guard, layout <Outlet/>)
│   ├── RequireAdmin.jsx             (Inc. 3 — reescrito como layout <Outlet/>, redirect a /unauthorized)
│   ├── SortableTh/SortableTh.jsx    (Inc. 2 — encabezado de columna ordenable)
│   ├── Pagination/Pagination.jsx    (Inc. 2 — control de paginación reutilizable)
│   └── WhatsAppButton/WhatsAppButton.jsx (Inc. 1 — botón flotante fijo bottom-right)
├── hooks/
│   └── useTableData.js              (Inc. 2 — ordenamiento y paginación cliente)
├── services/
│   ├── lodgingService.js            (Inc. 3 — nuevo)
│   ├── categoryService.js           (Inc. 3 — nuevo)
│   └── favoriteService.js           (Inc. 3 — nuevo)
├── pages/
│   ├── Booking/BookingPage.jsx, BookingConfirmation.jsx (Inc. 1)
│   ├── MyReservations/MyReservationsPage.jsx (Inc. 1)
│   ├── Unauthorized/Unauthorized.jsx (Inc. 3 — nuevo)
│   ├── SearchResults/SearchResults.jsx (Inc. 2 — Pagination unificada; Inc. 3 — reescrito server-driven)
│   └── Admin/
│       ├── AdminCategories.jsx, AdminFeatures.jsx, AdminPolicies.jsx, AdminUsers.jsx (Inc. 2 — useTableData)
│       ├── AdminLodgings.jsx, LodgingsTable.jsx (Inc. 2 — migradas a paginación cliente)
│       └── AdminDashboard.jsx (Inc. 2 — sección "Últimas reservas")
```

* **BookingPage en dos columnas (Inc. 1):** columna izquierda con datos del alojamiento, columna derecha con formulario y cálculo dinámico del total.
* **RequireAuth como route guard (Inc. 1):** redirige a `/login` preservando la ruta destino en el state (`from`).
* **`useTableData` (Inc. 2):** encapsula página actual, ordenamiento (`sortKey`, `direction`) y filtrado; provee `paginatedData` y handlers.
* **`SortableTh`/`Pagination` (Inc. 2):** aíslan la lógica visual, reutilizados en distintas partes del sistema.
* **Migración a client-side en Alojamientos (Inc. 2):** `AdminLodgings` pasó a `GET /api/lodgings` plano + `useTableData` local, homogeneizando con el resto de las entidades administrativas.
* **Capa de servicios por dominio (Inc. 3):** `lodgingService.searchLodgings(params)`, `categoryService.getCategories()`, `favoriteService.getFavorites()/addFavorite()/removeFavorite()` — desacoplan las llamadas HTTP directas de la UI. El alcance de esta primera capa se limitó a `SearchResults.jsx` como slice experimental (no se tocaron `FavoritesPage.jsx`/`ProductCard.jsx`, que siguen llamando `api.js` directo).
* **`SearchResults.jsx` server-driven (Inc. 3):** se eliminó la paginación local (`page`/`PAGE_SIZE=9` + slicing) y el filtrado en memoria de múltiples categorías (`runCategorySearch`); ahora un único `runSearch()` siempre envía las categorías seleccionadas al backend y la paginación se dirige por `currentPage`/`totalPages` de la respuesta. Los clics de paginación disparan un refetch real (vía `lastSearchRef`, que preserva los filtros aplicados), no un slice local.
* **`RequireAdmin` como layout (Inc. 3):** retorna `<Outlet/>` en vez de envolver `children`, igual que `RequireAuth`. El redirect para usuario autenticado sin rol ADMIN cambia de `/` a `/unauthorized` (página nueva, mínima, reutiliza estilos existentes de `App.css`).

## 3. Trazabilidad de Historias de Usuario (User Stories)

| ID | Historia de Usuario | Componente / Vista UI | Endpoint Backend | Criterio de Aceptación / Estado |
|----|---------------------|----------------------|------------------|--------------------------------|
| **US #30** | Seleccionar fecha de check-in/check-out con fechas ocupadas bloqueadas. | `BookingPage.jsx` | `GET /api/lodgings/{id}/availability` | Calendario con rangos CONFIRMED deshabilitados cargados al montar la página. |
| **US #31** | Visualizar detalles del alojamiento al iniciar la reserva. | `BookingPage.jsx` | `GET /api/lodgings/{id}` | Nombre, ciudad, precio, imagen, descripción y features visibles en columna izquierda. |
| **US #32** | Realizar la reserva y recibir confirmación en pantalla. | `BookingPage.jsx`, `BookingConfirmation.jsx` | `POST /api/reservations` | HTTP 201 → redirige a `/booking/confirmation` con nombre, fechas, huésped y total. |
| **US #33** | Acceder al historial personal de reservas. | `MyReservationsPage.jsx` | `GET /api/reservations/my` | Lista ordenada por `checkIn DESC`. Protegida por `RequireAuth`. |
| **US #34** | Iniciar conversación de WhatsApp con mensaje pre-cargado. | `WhatsAppButton.jsx` | N/A (Frontend) | Botón flotante visible para todos. Oculto si `VITE_WHATSAPP_NUMBER` no definida. |
| **US #35** | Recibir email de confirmación al realizar una reserva. | `BookingConfirmation.jsx` | `POST /api/reservations` (dispara `EmailService`) | `SmtpEmailServiceImpl` envía email HTML. `ConsoleEmailServiceImpl` loguea por defecto. |
| **US #36** | Visualizar tablas administrativas uniformes con ordenación y paginación local. | `AdminCategories`, `AdminFeatures`, `AdminPolicies`, `AdminUsers`, `AdminLodgings` | N/A (Frontend) | Tablas con estilos homogéneos, indicadores visuales de ordenación y paginado consistente. |
| **US #37** | Acceder a estadísticas de reservas y a la sección de reservas recientes. | `AdminDashboard.jsx` | `GET /api/reservations` | Tarjeta de estadística + tabla "Últimas reservas" (4 más recientes). Acceso restringido a ADMIN. |
| **US #38** | Buscar alojamientos filtrando por múltiples categorías, con resultados paginados desde el servidor. | `SearchResults.jsx` | `GET /api/lodgings/search` | Selección de 2+ categorías filtra en base de datos (no en memoria). Paginación dirigida por `currentPage`/`totalPages` del servidor. |
| **US #39** | Recibir mensajes de error en español o inglés según el idioma del navegador. | N/A (Backend, transversal) | Cualquier endpoint que dispare `ResourceNotFoundException`, `IllegalArgumentException`, o validación de `page`/`size` en `/search` | Header `Accept-Language: es` → mensaje en español; `en` o ausente → inglés. Logs siempre en inglés. |
| **US #40** | Acceso uniforme a rutas administrativas con redirección clara cuando falta permiso. | `RequireAdmin.jsx`, `Unauthorized.jsx` | N/A (Frontend) | Usuario autenticado sin rol ADMIN es redirigido a `/unauthorized` (antes: `/`, sin explicación). |

## 4. Catálogo de Endpoints Nuevos / Modificados

### 4.1. Reservas

| Método | Endpoint | Acceso (RBAC) | Descripción |
|--------|----------|---------------|-------------|
| POST | `/api/reservations` | Autenticado | Crear reserva (incluye `guestPhone`) |
| GET | `/api/reservations/my` | Autenticado | Historial del usuario autenticado, ordenado por `checkIn DESC` |
| GET | `/api/reservations` | ADMIN | Todas las reservas del sistema, ordenadas por `id DESC` (Inc. 2) |
| GET | `/api/lodgings/{id}/availability` | Público | Disponibilidad. Sin params: `occupiedRanges`. Con `checkIn`/`checkOut`: además `available`. |

### 4.2. Alojamientos

| Método | Endpoint | Acceso (RBAC) | Descripción |
|--------|----------|---------------|-------------|
| PUT | `/api/lodgings/{id}` | ADMIN | Usado desde `LodgingFormModal` en modo edición. |
| GET | `/api/lodgings/{id}` | Público | `LodgingDTO` incluye `averageRating`, `ratingCount`, y (Inc. 3) `features`/`policies` tipados. |
| GET | `/api/lodgings/search` | Público | **(Inc. 3, breaking change de contrato)** Acepta `categories` (múltiple), `page`, `size` (validados). Retorna `{lodgings, currentPage, totalItems, totalPages}` en vez del array plano anterior. |

<div style="page-break-before: always;"></div>

## 5. Modelo de Datos

### Modificaciones en Entidades y DTOs

```
┌─────────────────────────┐          ┌──────────────────────────┐
│       RESERVATION       │          │         CATEGORY          │
├─────────────────────────┤          ├──────────────────────────┤
│ id (PK, Long)           │          │ id (PK, Long)             │
│ lodging_id (FK)         │          │ name (NN)                 │
│ user_id (FK)            │          │ description               │
│ check_in (NN)           │          │ icon                      │
│ check_out (NN)          │          │ + image_url (nullable)    │
│ guest_name (NN)         │          └──────────────────────────┘
│ guest_email (NN)        │
│ + guest_phone (nullable)│          ┌──────────────────────────┐
│ total_price (NN)        │          │      LODGING DTO (+)      │
│ status (ENUM)           │          ├──────────────────────────┤
│ version (@Version)      │          │ (campos existentes)       │
└─────────────────────────┘          │ + averageRating (Double)  │
                                     │ + ratingCount (Integer)   │
                                     │ + features: List<FeatureSummaryDTO> (Inc. 3, era Map<String,Object>) │
                                     │ + policies: List<PolicySummaryDTO> (Inc. 3, era Map<String,Object>) │
                                     └──────────────────────────┘
```

* **Reservation:** `guestPhone` (String, nullable) para el formulario de reserva y contacto WhatsApp.
* **Category:** `imageUrl` (String, nullable) para `CategoryCard` y secciones de búsqueda.
* **LodgingDTO:** `averageRating`/`ratingCount` se calculan en `LodgingServiceImpl.enrichWithRatings()`, no son columnas persistidas.
* **`FeatureSummaryDTO` / `PolicySummaryDTO` (Inc. 3, nuevos):** `{id, name, icon}` y `{id, name, description, icon}` respectivamente. Reemplazan los `Map<String, Object>` genéricos, con mapeo manual (`fromEntity`) — se descartó MapStruct por directiva del proyecto.
* **`ResourceNotFoundException` (Inc. 3):** se agregaron campos opcionales `errorCode`/`args` (retrocompatibles) para soportar localización por clave en el futuro; el único sitio real que la lanza hoy (`ReservationServiceImpl`) sigue usando el constructor de mensaje plano.
* **Bundles de mensajes (Inc. 3, nuevos):** `messages.properties`/`messages_es.properties` con únicamente las claves que dispara código real: `error.page.negative`, `error.size.negative`, `error.resource.not_found`.

### Nuevas Queries en Repositorios

| Repositorio | Método | Propósito |
|-------------|--------|-----------|
| `ReservationRepository` | `findByUserIdOrderByCheckInDesc(Long)` | Historial de reservas del usuario |
| `ReservationRepository` | `existsByUserIdAndLodgingIdAndStatus(Long, Long, Status)` | Valida reserva CONFIRMED antes de permitir puntuación |
| `ReservationRepository` | `findAllByOrderByIdDesc()` | (Inc. 2) Listado admin de todas las reservas |
| `RatingRepository` | `countByLodgingId(Long)` | Conteo de reseñas para `ratingCount` |

## 6. Decisiones Técnicas Clave

* **Página dedicada vs modal para reservas (Inc. 1):** el criterio de la US #31 requiere que la información completa del alojamiento sea visible durante la reserva, difícil de lograr en un modal sin saturarlo.
* **`occupiedRanges` sin parámetros de fecha (Inc. 1):** permite que `BookingPage` bloquee fechas al cargar, sin esperar que el usuario seleccione un rango primero.
* **`@Primary` sobre `@ConditionalOnMissingBean` para EmailService (Inc. 1):** comportamiento determinista garantizado; el toggle se controla con `app.mail.smtp.enabled`.
* **WhatsApp como enlace `wa.me` (Inc. 1):** evita la dependencia de la API oficial de WhatsApp Business (cuenta verificada, aprobación de Meta).
* **Edición de alojamiento reutilizando `LodgingFormModal` (Inc. 1):** prop `lodging` opcional en vez de un componente separado, minimiza duplicación.
* **Ordenamiento y paginación en el cliente para tablas de administración (Inc. 2):** volúmenes de datos bajos/medios en un contexto exclusivamente admin — carga única simplifica el backend y da experiencia inmediata. Documentado explícitamente como reversible "si la volumetría de producción lo requiere" (ver Inc. 3 para el caso donde sí se justificó — búsqueda pública, no tablas admin).
* **Estandarización de `SortableTh`/`Pagination` (Inc. 2):** reutilizables entre distintas partes del sistema, incluida `SearchResults`.
* **TDD en la sección de reservas del dashboard (Inc. 2):** se implementó primero `ReservationControllerIntegrationTest` para asegurar `hasRole('ADMIN')` antes de construir servicio y UI.
* **Reutilizar el formato de respuesta paginada existente para `/search` (Inc. 3):** en vez de introducir un tercer formato de paginación, se adoptó el mismo `Map` (`lodgings`, `currentPage`, `totalItems`, `totalPages`) que ya usaba `GET /api/lodgings` — a costa de ser un cambio de contrato para `/search`, con un único consumidor conocido (`SearchResults.jsx`), actualizado en el mismo cambio.
* **`@Validated` + `@Min(message="{clave}")` en vez de excepción-con-mensaje-como-clave (Inc. 3):** el patrón de localización originalmente propuesto para `IllegalArgumentException` (tratar `ex.getMessage()` como clave de traducción) no es compatible con ningún punto real del código existente. Se usó Bean Validation, el mecanismo nativo de Spring para esto.
* **Alcance acotado de la i18n a 4 de 9 exception handlers (Inc. 3):** decisión explícita para no inflar el diff tocando código no relacionado con la búsqueda/paginación; documentado como deuda técnica controlada (Sección 8).
* **`RequireAdmin` como layout `<Outlet/>` (Inc. 3):** alinea con el patrón ya usado por `RequireAuth`, en vez de mantener dos convenciones distintas de route guard.

## 7. Testing

### 7.1. Cobertura Automatizada

* **Backend — 284 tests, todos en verde** (JUnit 5 + Mockito, integración con MockMvc + Testcontainers/MariaDB 10.11). Incluye, entre otros:
  - Incremento 1: `ReservationServiceImplTest`, `ReservationControllerIntegrationTest`, `ReservationOwnershipIntegrationTest` (5 escenarios IDOR), `ReservationConcurrencyTest`.
  - Incremento 2: `ReservationControllerIntegrationTest` — RBAC de `GET /api/reservations` (401/403/200) y ordenamiento `id DESC`.
  - Incremento 3: `LodgingDTOTest` (mapeo a DTOs tipados), `LodgingServiceImplTest`/`LodgingControllerIntegrationTest` (paginación, filtro multi-categoría, validación de `page`/`size`), `GlobalExceptionHandlerTest` (localización de los 4 handlers en scope), `ReservationControllerIntegrationTest` (localización del caso real de `ResourceNotFoundException`).
* **Frontend — 276 tests, todos en verde** (Vitest + React Testing Library, 38 archivos). Incluye, entre otros:
  - Incremento 1: `RequireAuth.test.jsx`, `BookingPage.test.jsx`, `BookingConfirmation.test.jsx`, `MyReservationsPage.test.jsx`, `Header.test.jsx`, `WhatsAppButton.test.jsx`.
  - Incremento 2: `useTableData.test.js`, `Pagination.test.jsx`, `AdminCategories/Features/Policies/Users/Lodgings.test.jsx`, `AdminDashboard.test.jsx`, `AdminReservations.test.jsx`.
  - Incremento 3: `lodgingService.test.js`, `categoryService.test.js`, `favoriteService.test.js` (31 tests nuevos), `SearchResults.test.jsx` (reescrito — filtrado server-side, respuesta paginada), `RequireAdmin.test.jsx` (redirect a `/unauthorized`), `Unauthorized.test.jsx` (nuevo).
* **Suite E2E con Playwright (Incremento 1, complementario):** 17 escenarios × 2 navegadores (Chromium + Firefox) = 34 ejecuciones, todas en verde al momento de su incorporación. No se re-ejecutó como parte del Incremento 3 (sin cambios en los flujos que cubre).

### 7.2. Hallazgos Durante el Incremento 3 (verificación empírica antes de asumir)

* El handler de `HandlerMethodValidationException` se mantiene registrado en `GlobalExceptionHandler` de forma defensiva, pero ningún endpoint real del proyecto lo dispara hoy — la validación de `page`/`size` vía `@Validated` a nivel de clase produce `ConstraintViolationException` (verificado empíricamente), no `HandlerMethodValidationException`.
* Sin un handler para esas excepciones, la validación regresaba HTTP 500 en vez de 400 (el catch-all `Exception.class` preexistente intercepta antes que la resolución nativa de Spring) — corregido con un handler dedicado.
* `spring.messages.fallback-to-system-locale=true` (default) producía resolución de idioma no determinística según el locale del sistema operativo del host — corregido explícitamente.

## 8. Limitaciones Conocidas y Deuda Técnica Controlada

1. **Paginación client-side en tablas de administración:** `AdminLodgings` y el resto de las entidades administrativas (Categorías, Características, Políticas, Usuarios) mantienen paginación y ordenamiento del lado del cliente. Si el volumen de datos creciera significativamente, se contempla migrar a paginación por base de datos — el Incremento 3 ya estableció el patrón para `/api/lodgings/search`, reutilizable si se decide extenderlo al panel admin.
2. **WhatsApp Business API:** el enlace `wa.me` no provee confirmación de envío ni manejo de errores desde la aplicación.
3. **Email SMTP desactivado por defecto:** requiere `MAIL_SMTP_ENABLED=true` y credenciales de Mailtrap.
4. **Precios por temporada:** el total de reserva es `días × pricePerNight`, sin tarifas variables por temporada o fin de semana.
5. **Refresh tokens:** el JWT expira a las 8 horas sin renovación automática.
6. **Gestión de reservas en admin:** el Dashboard es informativo; no hay acciones administrativas (cancelar, reprogramar) sobre reservas de usuarios.
7. **i18n acotada a 4 de 9 exception handlers (Inc. 3):** `AuthenticationException`, `MethodArgumentNotValidException`, `ObjectOptimisticLockingFailureException`, `PessimisticLockingFailureException`, `UploadException`, `DataIntegrityViolationException` y el catch-all genérico mantienen mensajes en español hardcodeado independientemente de `Accept-Language` — decisión explícita para acotar el alcance del cambio, no una omisión.
8. **`HandlerMethodValidationException` sin cobertura por request real (Inc. 3):** el handler existe de forma defensiva pero ningún endpoint actual lo dispara (ver 7.2); cubierto solo por test unitario directo.
9. **CI de GitHub Actions no corre sobre `sprint-4`:** el workflow (`.github/workflows/ci.yml`) solo dispara en push/PR contra `main`. Las 6 PRs del Incremento 3 se verificaron con las suites de test locales (284 backend + 276 frontend) antes de cada merge, sin la capa adicional de validación en un runner limpio de GitHub Actions.
10. **Gap preexistente de configuración de ESLint (detectado, no introducido por el Incremento 3):** falta configurar `no-undef` para los globals de test (`describe`/`it`/`expect`/`vi`), afecta a prácticamente todos los archivos de test del repositorio.
