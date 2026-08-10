---
title: "Bitácora de Ejecución y Cierre — Backend Security Hardening"
subtitle: "TuHospedaje — Seguridad, Integridad de Datos y Consistencia de API"
author: "Equipo de Desarrollo"
date: "Junio 2026"
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
      <div>Backend Security Hardening — Junio 2026</div>
      <div>Página <span class="pageNumber"></span> de <span class="totalPages"></span></div>
    </div>
---

<style>
.page-break { page-break-before: always; }
table { width: 100%; } table, tr { page-break-inside: avoid; }
h1, h2, h3, h4 { page-break-after: avoid; }
</style>

# BITÁCORA DE EJECUCIÓN Y CIERRE — BACKEND SECURITY HARDENING

**Foco del Incremento:** Seguridad de API, integridad transaccional, eliminación de N+1 y consistencia de semántica HTTP
**Stack Tecnológico:** Java 17 / Spring Boot 3.5 / Spring Security 6 / MariaDB / JPA (Hibernate) / Testcontainers / JUnit 5
**Branches:** `feat/security-hardening-pr1`, `feat/security-hardening-pr2`, `feat/security-hardening-pr3` (stacked-to-main)

## 1. Resumen del Incremento (Scope)

Este incremento post-Sprint 4 apunta exclusivamente al backend. Fue motivado por una auditoría arquitectónica integral que identificó una vulnerabilidad de seguridad real (IDOR), una race condition que podía generar reservas duplicadas bajo concurrencia, un problema de rendimiento sistémico (N+1 queries), y múltiples inconsistencias de API y configuración.

El incremento se organizó en tres pull requests encadenados (stacked-to-main) para mantener los diffs revisables:

- **PR-1** — Fundación transaccional y lock pesimista: `@Transactional` en todos los service implementations y protección de la operación de reserva contra doble booking concurrente.
- **PR-2** — Seguridad de acceso, rendimiento y excepciones: IDOR fix en reservas, eliminación del N+1 de ratings mediante query agregada, migración a `FetchType.LAZY` en tres colecciones, y manejo estructurado de excepciones.
- **PR-3** — Configuración segura y semántica HTTP: eliminación de defaults de secretos en producción, perfil de desarrollo separado, externalización de CORS, y normalización de códigos HTTP en controladores.

El cambio no modifica el frontend ni el esquema de base de datos.

## 2. Arquitectura Afectada

### 2.1. Flujo de Request (sin cambios estructurales)

```
HTTP → JwtAuthenticationFilter → SecurityFilterChain
     → @RestController → Service (Interface + Impl)
     → JpaRepository → MariaDB
```

Los cambios refuerzan cada capa sin alterar la estructura:

- **Filtro de seguridad:** Entry points diferenciados por ruta (401 para `/api/reservations/**`, 403 para el resto).
- **Service layer:** Todas las operaciones de escritura anotadas con `@Transactional`; `createReservation` con lock pesimista.
- **Repository layer:** Nuevo método con `@Lock(PESSIMISTIC_WRITE)` en `ReservationRepository`; query agregada en `RatingRepository`.
- **Entity layer:** `FetchType.LAZY` explícito en colecciones many-to-many.
- **Config layer:** Nuevo record `CorsProperties` con `@ConfigurationProperties`; perfil `dev` separado.

### 2.2. Matriz de Componentes Modificados

| Capa | Componente | Tipo de Cambio |
|------|-----------|----------------|
| Service | `ReservationServiceImpl` | `@Transactional`, ownership check IDOR, lock pesimista en `createReservation` |
| Service | `LodgingServiceImpl` | `@Transactional`/`readOnly`, batch `enrichWithRatings` reemplaza N+1 |
| Service | `UserServiceImpl`, `RatingServiceImpl`, `AuthServiceImpl`, `CategoryServiceImpl`, `FeatureServiceImpl`, `PolicyServiceImpl` | `@Transactional` / `readOnly` por método |
| Service | `CloudinaryServiceImpl` | Lanza `UploadException` tipada en lugar de `RuntimeException` |
| Repository | `ReservationRepository` | Nuevo método `lockByLodgingIdAndStatus` con `@Lock(PESSIMISTIC_WRITE)` |
| Repository | `RatingRepository` | Nueva projection `RatingAggregate` + método `aggregateByLodgingIds(Set<Long>)` |
| Entity | `Lodging` | `features` y `policies` con `fetch = FetchType.LAZY` |
| Entity | `User` | `favorites` con `fetch = FetchType.LAZY` |
| Exception | `GlobalExceptionHandler` | Handlers para catch-all (500), `PessimisticLockingFailureException` (409), `UploadException` (502) |
| Exception | `UploadException` | Nueva excepción tipada (nueva clase) |
| Controller | `LodgingController` | `@Valid` en `update`; `delete` retorna 204 |
| Controller | `FavoriteController` | `addFavorite` retorna 201; `removeFavorite` retorna 204 |
| Config | `SecurityConfig` | CORS vía `CorsProperties`; entry points diferenciados |
| Config | `JwtService` | `expirationMillis` inyectado vía `@Value("${app.jwt.expiration}")` |
| Config | `CorsProperties` | Nuevo record `@ConfigurationProperties(prefix = "app.cors")` (nueva clase) |
| Config | `BackendApplication` | `@ConfigurationPropertiesScan` |
| Resources | `application.properties` | Sin defaults en secretos; CORS externalizado; `show-sql` removido |
| Resources | `application-dev.properties` | Nuevo — valores locales de desarrollo |
| Resources | `src/test/resources/application.properties` | Nuevo — propiedades de contexto de test |

<div style="page-break-before: always;"></div>

## 3. Detalle de Cambios por Área

### 3.1. Race Condition en Reservas (Double Booking)

**Problema:** `ReservationServiceImpl.createReservation` realizaba el chequeo de solapamiento de fechas y el `save` en dos operaciones separadas sin transacción. Dos requests concurrentes para el mismo hospedaje y fechas podían pasar ambos el chequeo e insertar dos reservas confirmadas. El campo `@Version` presente en la entidad `Reservation` no protege este caso: dos INSERTs nuevos nunca conflictúan por versión.

**Solución:**
1. `@Transactional` en `createReservation` como boundary atómico.
2. Nuevo método en `ReservationRepository` con `@Lock(LockModeType.PESSIMISTIC_WRITE)` que bloquea las filas de reservas del hospedaje durante la lectura del chequeo.
3. Lock timeout de 3000 ms configurado en `application.properties` (`spring.jpa.properties.jakarta.persistence.lock.timeout=3000`).

**Verificación empírica:** El test de concurrencia (`ReservationConcurrencyTest`) confirmó que el gap locking de InnoDB sobre el índice FK de `lodging_id` serializa correctamente dos inserts en un hospedaje sin reservas previas, sin necesidad del fallback de lockear la fila padre.

```
ReservationRepository.java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT r FROM Reservation r WHERE r.lodging.id = :lodgingId AND r.status = :status")
List<Reservation> lockByLodgingIdAndStatus(@Param("lodgingId") Long lodgingId,
                                           @Param("status") ReservationStatus status);
```

### 3.2. IDOR en GET /api/reservations/{id}

**Problema:** Cualquier usuario autenticado podía consultar la reserva de cualquier otro usuario incrementando el ID en la URL, exponiendo nombre, teléfono, email y precio.

**Solución:** Chequeo de ownership en `ReservationServiceImpl.getReservationById`: si el usuario autenticado no es el dueño de la reserva ni tiene rol ADMIN, se lanza `ResourceNotFoundException` → HTTP 404. La respuesta 404 (en lugar de 403) oculta la existencia del recurso, previniendo enumeración de IDs.

### 3.3. Eliminación del N+1 de Ratings

**Problema:** `LodgingServiceImpl.enrichWithRatings` emitía una query `SELECT` por cada hospedaje en cada operación de lista o búsqueda. Con N hospedajes, cada llamada a `findAll`, `findByCategory`, `search`, `findAllRandom` o `findAllPaginated` disparaba N+1 queries.

**Solución:** Una sola query JPQL con agregación (`AVG`, `COUNT`, `GROUP BY`) sobre un conjunto de IDs reemplaza el bucle:

```java
// RatingRepository.java
@Query("SELECT r.lodging.id AS lodgingId, AVG(r.score) AS average, COUNT(r) AS count " +
       "FROM Rating r WHERE r.lodging.id IN :ids GROUP BY r.lodging.id")
List<RatingAggregate> aggregateByLodgingIdsQuery(@Param("ids") Set<Long> ids);
```

El método público `aggregateByLodgingIds` incluye un guard para colecciones vacías (evita `IN ()` que falla en MariaDB). El redondeo `Math.round(avg * 10.0) / 10.0` y el manejo de hospedajes sin ratings (avg=0.0, count=0) son idénticos al comportamiento anterior.

### 3.4. FetchType.LAZY en Colecciones @ManyToMany

**Problema:** `Lodging.features`, `Lodging.policies` y `User.favorites` eran `@ManyToMany` sin `FetchType` explícito. El default de JPA para `@ManyToMany` es `EAGER`, lo que provoca queries adicionales automáticas en cada `findById` o listado.

**Solución en el corte original:** se hizo explícito `fetch = FetchType.LAZY` en las tres colecciones. OSIV (open-session-in-view) permanecía habilitado en ese momento y su desactivación quedó registrada como deuda técnica controlada. La resolución posterior está documentada en ADR-2.

### 3.5. Configuración Segura

**Problema:** `application.properties` tenía defaults hardcodeados como fallback para `JWT_SECRET` (token forjable si la variable de entorno no se seteaba), `DB_USERNAME`/`DB_PASSWORD`, y el origen de CORS `http://localhost:5173` (podía filtrarse a producción).

**Solución:**
- Variables de entorno sin default: `${JWT_SECRET}`, `${DB_USERNAME}`, `${DB_PASSWORD}`, `${CORS_ALLOWED_ORIGINS}` — la aplicación falla al arrancar si no están presentes.
- `application-dev.properties` provee los valores locales de desarrollo.
- `CorsProperties` record con `@ConfigurationProperties(prefix = "app.cors")` — no más strings hardcodeados en `SecurityConfig`.
- `app.jwt.expiration` (ya declarado) ahora se inyecta en `JwtService` vía `@Value`, eliminando el número mágico `28800000`.
- `spring.jpa.show-sql` y `format_sql` movidos al perfil dev.

<div style="page-break-before: always;"></div>

## 4. Catálogo de Endpoints Modificados

| Método | Endpoint | Cambio |
|--------|----------|--------|
| GET | `/api/reservations/{id}` | Ahora retorna 404 si el usuario autenticado no es el dueño ni ADMIN (antes: 200 para cualquier usuario autenticado) |
| DELETE | `/api/lodgings/{id}` | Retorna 204 No Content (antes: 200 + string body) |
| POST | `/api/favorites/{lodgingId}` | Retorna 201 Created (antes: 200 OK) |
| DELETE | `/api/favorites/{id}` | Retorna 204 No Content (antes: 200 + string body) |
| PUT | `/api/lodgings/{id}` | Body ahora validado con Bean Validation (`@Valid`); antes podía enviarse sin validar |

**Consistencia final de DELETE:** Category, Feature, Policy, Lodging y Favorite retornan todos 204 No Content.

## 5. Decisiones Técnicas Clave (ADRs)

### ADR-1: Lock Pesimista sobre Lock Optimista para Reservas

Se eligió `@Lock(PESSIMISTIC_WRITE)` para el chequeo de solapamiento en lugar de confiar únicamente en `@Version` (optimistic locking). El campo `@Version` protege actualizaciones concurrentes sobre una misma fila existente, pero dos INSERTs de nuevas reservas nunca conflictúan en versión. Solo el lock pesimista serializa correctamente el check-then-insert.

### ADR-2: OSIV deshabilitado (resolución posterior)

`spring.jpa.open-in-view=false`. El refactor futuro mencionado originalmente en este ADR ya se completó: una auditoría confirmó que todos los controllers devuelven DTOs (nunca entidades) y que todo el mapeo Entity→DTO ocurre dentro de fronteras `@Transactional`/`@Transactional(readOnly=true)`, antes de que la transacción cierre — incluyendo el acceso a colecciones lazy (`Lodging.features`, `Lodging.policies`, `User.favorites`, etc.), cubierto por `LazyFetchIntegrationTest`. Con esas garantías ya en código, mantener OSIV habilitado solo agregaba el costo conocido del anti-patrón (conexión de DB retenida durante toda la request, riesgo de N+1 silencioso) sin necesidad real.

### ADR-3: HTTP 404 en lugar de 403 para IDOR

Para recursos de usuario (reservas), responder 403 confirmaría al atacante que el recurso existe. HTTP 404 oculta la existencia del ID, previniendo enumeración. Esta es la práctica de GitHub, Stripe y otros servicios de referencia.

### ADR-4: Todos los DELETE retornan 204 No Content

Normalización completa: Category, Feature, Policy, Lodging y Favorite retornan 204. Un DELETE exitoso no tiene contenido que devolver — el status code comunica el resultado sin necesidad de body. El frontend no consume el body de los DELETEs en ningún punto del código actual.

### ADR-5: Sin defaults en secretos de producción

`JWT_SECRET`, `DB_USERNAME`, `DB_PASSWORD` y `CORS_ALLOWED_ORIGINS` no tienen fallback en `application.properties`. Un arranque sin estas variables falla explícitamente (fail-fast), lo que es preferible a correr silenciosamente con valores de desarrollo en producción.

## 6. Testing

### 6.1. Tests Nuevos

| Test | Escenarios cubiertos | Tecnología |
|------|---------------------|------------|
| `ReservationConcurrencyTest` | SC-1.2/1.3: dos threads concurrentes, solo uno éxito; SC-1.4: checkout==checkin no es solapamiento; SC-1.5: fechas adyacentes | Testcontainers (MariaDB real) + `CountDownLatch` |
| `ReservationOwnershipIntegrationTest` | SC-2.1: owner → 200; SC-2.2: ADMIN → 200; SC-2.3/2.4: non-owner y no-auth → 404/401; SC-2.5: IDOR explícito (user A pide reserva de user B → 404) | Testcontainers |
| `GlobalExceptionHandlerTest` | SC-6.1: excepción no manejada → 500 + JSON estándar; SC-6.3: PessimisticLockingFailure → 409 | MockMvc |
| `UploadExceptionHandlerTest` | SC-6.2: UploadException → 502 + JSON estándar | MockMvc |
| `RatingAggregateRepositoryTest` | Query aggregate correcta, guard de colección vacía, rounding, hospedajes sin ratings | Testcontainers |
| `RatingBatchEnrichmentIntegrationTest` | Comportamiento idéntico al N+1 anterior en valores devueltos | Testcontainers |
| `LazyFetchIntegrationTest` | SC-4.1–4.4: sin LazyInitializationException en ningún endpoint tras el switch | Testcontainers |
| `JwtExpirationPropertyTest` | SC-5.4/5.5: `app.jwt.expiration` inyectado correctamente | Spring context |
| `LodgingHttpSemanticsTest` | SC-7.1: PUT inválido → 400; SC-7.3: DELETE → 204 | MockMvc |
| `FavoriteHttpSemanticsTest` | SC-7.4: POST → 201; SC-7.5: DELETE → 204 | MockMvc |

### 6.2. Resultado Final del Suite

```
Tests run: 193, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Los 8 tests pre-existentes que fallaban (esperaban 403, recibían 401 para requests no autenticados) quedaron resueltos como parte del fix de TASK-7 al introducir un `DelegatingAuthenticationEntryPoint` diferenciado por ruta.

## 7. Limitaciones Conocidas y Deuda Técnica Controlada

1. **N+1 de disponibilidad en `search()`:** `LodgingServiceImpl.search` filtra disponibilidad por fechas llamando a `findByLodgingIdAndStatus` una vez por hospedaje resultante. Este patrón N+1 sobre reservas (distinto al N+1 de ratings, ya corregido) queda pendiente para un pase de optimización futuro con una subquery `NOT EXISTS` en la Specification.

2. **Mensaje de excepción en `UploadException` handler:** `GlobalExceptionHandler` devuelve `ex.getMessage()` como campo `error`. Hoy es seguro porque `CloudinaryServiceImpl` usa un mensaje constante, pero en el futuro convendría hardcodear el mensaje en el handler para blindarlo ante cambios en la excepción.

3. **OSIV habilitado (hallazgo histórico):** en el corte original, `spring.jpa.open-in-view=true` era la configuración explícita y su desactivación estaba registrada como refactor futuro. Una auditoría posterior completó ese refactor: la configuración actual es `spring.jpa.open-in-view=false` y `LazyFetchIntegrationTest` verifica el acceso a colecciones lazy dentro de fronteras transaccionales.

4. **Paginación obligatoria en `GET /api/lodgings`:** `findAll()` sin límite sigue siendo el comportamiento cuando no se pasan parámetros de página. El impacto de rendimiento está mitigado por el fix del N+1 de ratings y el LAZY de colecciones, pero la paginación obligatoria sigue siendo la solución correcta a largo plazo.
