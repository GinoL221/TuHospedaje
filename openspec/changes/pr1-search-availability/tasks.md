# Tasks: PR 1 Search and Availability

## Review Workload Forecast

| Field | Value |
|---|---|
| Total files modified/created | ~32-38 |
| Estimated changed lines | ~850-1150 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1A backend → PR 1B frontend |
| Delivery strategy | ask-on-risk |
| Chain strategy | feature-branch-chain |

Decision: Split into PR 1A (backend) + PR 1B (frontend)
PR 1A base: sprint-3 (tracker)
PR 1B base: branch of PR 1A
Chain strategy: feature-branch-chain

## ▶️ PR 1A — Backend (modelo + API + tests)

### Batch 1: Modelo de datos

- **T1.1** Extender `Lodging` con `pricePerNight`, `maxGuests`, `@Version` y ajustar mapeo estático en `LodgingDTO`.  
  **Archivos**: `backend/src/main/java/com/tuhospedaje/entity/Lodging.java`, `backend/src/main/java/com/tuhospedaje/dto/LodgingDTO.java`. **Esfuerzo**: Medium. **Dependencias**: —.  
  **Tests**: `backend/src/test/java/com/tuhospedaje/lodging/{LodgingDTOTest,LodgingEntityMappingTest}.java`.

- **T1.2** Crear `Reservation`, `ReservationStatus` y DTOs de request/response/availability con validaciones de fechas y huésped.  
  **Archivos**: `backend/src/main/java/com/tuhospedaje/entity/Reservation.java`, `backend/src/main/java/com/tuhospedaje/enums/ReservationStatus.java`, `backend/src/main/java/com/tuhospedaje/dto/reservation/*.java`. **Esfuerzo**: Medium. **Dependencias**: T1.1.  
  **Tests**: `backend/src/test/java/com/tuhospedaje/reservation/ReservationDtoValidationTest.java`.

- **T1.3** Agregar `JpaSpecificationExecutor<Lodging>` al repositorio y derived queries de filtros (`findByPricePerNightBetween`, `findByMaxGuestsGreaterThanEqual`, `findByCityContainingIgnoreCase`).  
  **Archivos**: `backend/src/main/java/com/tuhospedaje/repository/{LodgingRepository,ReservationRepository}.java`. **Esfuerzo**: Medium. **Dependencias**: T1.2.  
  **Tests**: `backend/src/test/java/com/tuhospedaje/lodging/LodgingSearchIntegrationTest.java`.

## Batch 2: Endpoints de búsqueda y reserva

- **T2.1** Implementar en servicio búsqueda unificada, ciudades y disponibilidad por rango.  
  **Archivos**: `backend/src/main/java/com/tuhospedaje/service/LodgingService.java`, `backend/src/main/java/com/tuhospedaje/service/impl/LodgingServiceImpl.java`. **Esfuerzo**: Large. **Dependencias**: T1.3.  
  **Tests**: ampliar `backend/src/test/java/com/tuhospedaje/lodging/LodgingServiceImplTest.java` (RED→GREEN→REFACTOR).

- **T2.2** Crear `ReservationService` para `POST/GET`, cálculo de total, control de solapamiento y conflicto concurrente.  
  **Archivos**: `backend/src/main/java/com/tuhospedaje/service/{ReservationService}.java`, `backend/src/main/java/com/tuhospedaje/service/impl/{ReservationServiceImpl}.java`. **Esfuerzo**: Large. **Dependencias**: T1.3, T2.1.  
  **Tests**: `backend/src/test/java/com/tuhospedaje/reservation/ReservationServiceImplTest.java`.

- **T2.3** Actualizar `LodgingController` y crear `ReservationController` con contrato final y `@PreAuthorize` en `POST /api/reservations`.  
  **Archivos**: `backend/src/main/java/com/tuhospedaje/controller/{LodgingController,ReservationController}.java`. **Esfuerzo**: Medium. **Dependencias**: T2.1, T2.2.  
  **Tests**: `backend/src/test/java/com/tuhospedaje/{lodging,reservation}/*ControllerIntegrationTest.java`.

- **T2.4** Mapear `OptimisticLockException` a `409` y confirmar reglas de acceso público/privado.  
  **Archivos**: `backend/src/main/java/com/tuhospedaje/exception/GlobalExceptionHandler.java`, `backend/src/main/java/com/tuhospedaje/configuration/SecurityConfig.java`. **Esfuerzo**: Small. **Dependencias**: T2.3.  
  **Tests**: integración de conflicto y auth en reservas.

## ▶️ PR 1B — Frontend (búsqueda + disponibilidad)

### Batch 3: Frontend búsqueda

- **T3.1** Extender `api.js` para llamadas públicas (`search/cities/availability`) y reservas autenticadas.  
  **Archivos**: `frontend/src/services/api.js`. **Esfuerzo**: Small. **Dependencias**: T2.3.  
  **Tests**: smoke manual + `npm run lint`.

- **T3.2** Implementar Home con autocomplete (debounce 300ms), validación check-in/check-out y navegación a `/search`.  
  **Archivos**: `frontend/src/pages/Home/{Home.jsx,Home.css}`, `frontend/src/components/search/{SearchBar.jsx,CityAutocomplete.jsx}`. **Esfuerzo**: Medium. **Dependencias**: T3.1.  
  **Tests**: escenarios de `specs/frontend-search/spec.md`.

- **T3.3** Crear `/search` con lectura de query params, filtros y estado vacío; registrar ruta.  
  **Archivos**: `frontend/src/pages/SearchResults/{SearchResults.jsx,SearchResults.css}`, `frontend/src/components/search/{SearchFilters.jsx,SearchResultList.jsx}`, `frontend/src/App.jsx`. **Esfuerzo**: Medium. **Dependencias**: T3.2.  
  **Tests**: smoke manual URL compartible/filtros.

## Batch 4: Frontend disponibilidad

- **T4.1** Instalar `react-datepicker`; mostrar precio/noche, total estimado y calendario con fechas ocupadas/pasadas bloqueadas.  
  **Archivos**: `frontend/package.json`, `frontend/src/pages/ProductDetail/{ProductDetail.jsx,ProductDetail.css}`, `frontend/src/components/reservation/AvailabilityCalendar.jsx`. **Esfuerzo**: Medium. **Dependencias**: T3.1, T2.3.  
  **Tests**: smoke manual de `specs/frontend-availability/spec.md`.

- **T4.2** Crear `ReservationModal` con confirmación, submit a `/api/reservations`, éxito y error de conflicto.  
  **Archivos**: `frontend/src/components/reservation/ReservationModal.jsx`, `frontend/src/pages/ProductDetail/ProductDetail.jsx`. **Esfuerzo**: Medium. **Dependencias**: T4.1, T2.4.  
  **Tests**: smoke manual reserva exitosa/conflicto.
