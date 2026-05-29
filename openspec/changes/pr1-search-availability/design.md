# Design: PR 1 Search and Availability

## Technical Approach

Extend the existing lodging CRUD flow instead of adding a parallel module. Backend keeps the current Controller → Service → Repository + mutable DTO pattern, adds reservation persistence, and exposes public search/availability plus authenticated reservation creation. Frontend keeps route-level pages and `services/api.js`, wiring Home → `/search` and ProductDetail → reservation modal.

## Architecture Decisions

| Decision | Choice | Alternatives considered | Rationale |
|---|---|---|---|
| DTO mapping | Keep `toEntity/fromEntity` static DTO mapping | New mapper layer | Current code already maps in DTOs; changing pattern now adds noise to PR 1. |
| Availability rule | Overlap = `checkIn < existing.checkOut AND checkOut > existing.checkIn` | Inclusive end-date blocking | Matches hotel semantics: check-out day becomes available. |
| Concurrency | Version on `Lodging` plus pre-save overlap check; `Reservation` keeps `@Version` for future updates | Reservation-only version, pessimistic lock | Reservation-only version does NOT stop two inserts; lodging version gives a single aggregate to contend on without DB-specific locks. |

## Data Flow

```text
Home/SearchPage ──GET /lodgings/cities,/lodgings/search──> LodgingController
ProductDetail ──GET /lodgings/{id}/availability──────────> LodgingService
ReservationModal ──POST /reservations────────────────────> ReservationService
ReservationService ──check overlap + bump lodging version─> DB
```

```text
Category 1 ── * Lodging 1 ── * LodgingImage
Feature * ── * Lodging 1 ── * Reservation * ── 1 User
```

`Lodging` gains `pricePerNight BigDecimal` and `maxGuests Integer`; add `@Version Long version`. `Reservation` stores `lodging`, authenticated `user`, `checkIn`, `checkOut`, `guestName`, `guestEmail`, `totalPrice`, `status`, `version`.

## File Changes

| File | Action | Description |
|---|---|---|
| `backend/src/main/java/com/tuhospedaje/entity/Lodging.java` | Modify | Add price/capacity, reservation collection, version. |
| `backend/src/main/java/com/tuhospedaje/entity/Reservation.java` | Create | New aggregate with `@Version` and status enum. |
| `backend/src/main/java/com/tuhospedaje/enums/ReservationStatus.java` | Create | `CONFIRMED`, `CANCELLED`. |
| `backend/src/main/java/com/tuhospedaje/dto/LodgingDTO.java` | Modify | Include price/capacity. |
| `backend/src/main/java/com/tuhospedaje/dto/reservation/*` | Create | Request/response + availability/search DTOs. |
| `backend/src/main/java/com/tuhospedaje/controller/{LodgingController,ReservationController}.java` | Modify/Create | Public search endpoints, authenticated POST reservation. |
| `backend/src/main/java/com/tuhospedaje/service/{LodgingService,ReservationService}.java` | Modify/Create | Search, availability, create/get reservation APIs. |
| `backend/src/main/java/com/tuhospedaje/service/impl/{LodgingServiceImpl,ReservationServiceImpl}.java` | Modify/Create | JPQL search, overlap validation, optimistic retry surface. |
| `backend/src/main/java/com/tuhospedaje/repository/{LodgingRepository,ReservationRepository}.java` | Modify/Create | Search JPQL, city autocomplete, overlap queries. |
| `backend/src/main/java/com/tuhospedaje/exception/GlobalExceptionHandler.java` | Modify | Map optimistic locking to HTTP 409. |
| `frontend/src/pages/{Home/Home.jsx,ProductDetail/ProductDetail.jsx}` | Modify | Search form and reservation flow. |
| `frontend/src/pages/SearchResults/{SearchResults.jsx,SearchResults.css}` | Create | Query-param driven results + filters. |
| `frontend/src/components/search/*` | Create | `SearchBar`, `CityAutocomplete`, `SearchFilters`, `SearchResultList`. |
| `frontend/src/components/reservation/*` | Create | `AvailabilityCalendar`, `ReservationModal`. |
| `frontend/src/services/api.js` | Modify | Add unauthenticated GET helper and reservation endpoints. |
| `backend/src/test/java/com/tuhospedaje/{lodging,reservation}/*` | Modify/Create | Unit + MockMvc/Testcontainers coverage. |

## Interfaces / Contracts

`GET /api/lodgings/search?city&checkIn&checkOut&guests&category&minPrice&maxPrice&featureIds`
→ `200 [{id,name,city,country,pricePerNight,maxGuests,imageUrls,categoryName,features}]`; invalid dates `400`.

`GET /api/lodgings/cities?q=` → `200 ["Bariloche"]`.

`GET /api/lodgings/{id}/availability?checkIn&checkOut`
→ `200 {available, occupiedRanges:[{checkIn,checkOut}]}`; missing/invalid dates `400`; unknown lodging `404`.

`POST /api/reservations` (`@PreAuthorize("isAuthenticated()")`)
body `{lodgingId,checkIn,checkOut,guestName,guestEmail}`
→ `201 {id,status,totalPrice,lodgingId,userId,checkIn,checkOut,guestName,guestEmail,version}`; overlap/conflict `409`; validation `400`; unknown lodging `404`.

`GET /api/reservations/{id}` → `200 ReservationResponseDTO`, `404`.

Validation: `pricePerNight >= 0`, `maxGuests > 0`, `checkIn >= today`, `checkOut > checkIn`, guest name non-blank max 100, email valid.

Search via **Specifications** (Criteria API):

```java
Specification<Lodging> spec = Specification.where(null);

if (city != null)
    spec = spec.and((root, query, cb) ->
        cb.like(cb.lower(root.get("city")), "%" + city.toLowerCase() + "%"));
if (guests != null)
    spec = spec.and((root, query, cb) ->
        cb.greaterThanOrEqualTo(root.get("maxGuests"), guests));
// ... resto de filtros encadenados

List<Lodging> results = lodgingRepository.findAll(spec);
```

Overlap check in service usando `ReservationRepository` con derived query o stream en memoria.

## Testing Strategy

| Layer | What to Test | Approach |
|---|---|---|
| Unit | `LodgingServiceImpl` search params, city autocomplete; `ReservationServiceImpl` total price, overlap rejection, version conflict translation | Mockito, new tests near existing service tests. |
| Integration | `/lodgings/search`, `/cities`, `/availability`, `/reservations` auth/validation/conflict | `@SpringBootTest` + `MockMvc` + Testcontainers MariaDB. |
| Concurrency | Double booking same lodging/dates | Two transactions/threads against same lodging version; expect one `201`, one `409`. |

## Migration / Rollout

No destructive migration. `ddl-auto=update` adds columns/tables; seed/test fixtures must start providing `pricePerNight` and `maxGuests`.

## Open Questions

- [ ] Add `react-datepicker` dependency now; frontend currently has no calendar package installed.
