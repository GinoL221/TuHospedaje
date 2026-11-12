# API Reference

All endpoints live under the base path `/api`. The JSON body shape of every error
response is documented in [api-error-contract.md](api-error-contract.md); this file does
not repeat it.

**Authentication** is an httpOnly `ACCESS_TOKEN` cookie set by `/api/auth/login`,
`/api/auth/register` and `/api/auth/refresh`. There is no bearer token. Unsafe methods
(POST, PUT, PATCH, DELETE) additionally require a CSRF token: read it from the
`XSRF-TOKEN` cookie (bootstrap it with `GET /api/auth/csrf`) and echo it in the
`X-XSRF-TOKEN` header. Three entry points are CSRF-exempt by design —
`POST /api/auth/login`, `POST /api/auth/register` and `POST /api/auth/refresh` — because
their real credential is a cookie itself; `POST /api/auth/welcome-email/resend` is
CSRF-exempt only while the caller is unauthenticated.

## Endpoint table

One row per controller handler. The table has 50 rows, one for each handler in the
generated OpenAPI spec (`/v3/api-docs`); a test
(`ApiReferenceDocumentationIntegrationTest`) keeps this table in sync with the spec.

| Method | Path | Access | Request | Response | Statuses |
| --- | --- | --- | --- | --- | --- |
| POST | /api/auth/register | public | RegisterRequest | AuthResponse | `201`, `400`, `409`, `429` |
| POST | /api/auth/login | public | LoginRequest | AuthResponse | `200`, `400`, `401`, `429` |
| POST | /api/auth/logout | public +CSRF | none | none | `204`, `403` |
| GET | /api/auth/me | authenticated | none | AuthResponse | `200`, `401` |
| POST | /api/auth/welcome-email/resend | authenticated +CSRF | none | none | `200`, `202`, `401`, `429` |
| GET | /api/auth/csrf | authenticated | none | none | `204`, `401` |
| POST | /api/auth/refresh | public | none | AuthResponse | `200`, `401`, `429` |
| POST | /api/auth/password | authenticated +CSRF | PasswordChangeRequest | none | `204`, `400`, `403` |
| POST | /api/categories | admin +CSRF | CategoryDTO | CategoryDTO | `201`, `400`, `403`, `409` |
| GET | /api/categories | public | none | `List<CategoryDTO>` | `200` |
| GET | /api/categories/{id} | public | path | CategoryDTO | `200`, `404` |
| PUT | /api/categories/{id} | admin +CSRF | CategoryDTO | CategoryDTO | `200`, `400`, `403`, `404`, `409` |
| DELETE | /api/categories/{id} | admin +CSRF | path | none | `204`, `400`, `403`, `404` |
| POST | /api/favorites/{lodgingId} | authenticated +CSRF | path | none | `201`, `403`, `404` |
| DELETE | /api/favorites/{lodgingId} | authenticated +CSRF | path | none | `204`, `403`, `404` |
| GET | /api/favorites | authenticated | none | `List<LodgingDTO>` | `200`, `403` |
| POST | /api/features | admin +CSRF | FeatureDTO | FeatureDTO | `201`, `400`, `403`, `409` |
| GET | /api/features | public | none | `List<FeatureDTO>` | `200` |
| GET | /api/features/{id} | public | path | FeatureDTO | `200`, `404` |
| PUT | /api/features/{id} | admin +CSRF | FeatureDTO | FeatureDTO | `200`, `400`, `403`, `404`, `409` |
| DELETE | /api/features/{id} | admin +CSRF | path | none | `204`, `403`, `404` |
| POST | /api/lodgings | admin +CSRF | LodgingDTO | LodgingDTO | `201`, `400`, `403`, `409` |
| PUT | /api/lodgings/{id} | admin +CSRF | LodgingDTO | LodgingDTO | `200`, `400`, `403`, `404`, `409` |
| GET | /api/lodgings | public | query | `List<LodgingDTO>` or `PageResponse<LodgingDTO>` | `200`, `400` |
| GET | /api/lodgings/recommendations | public | query | RecommendationPageResponse | `200`, `400` |
| GET | /api/lodgings/random | public | none | `List<LodgingDTO>` | `200` |
| GET | /api/lodgings/admin | admin | query | `PageResponse<LodgingDTO>` | `200`, `400`, `403` |
| GET | /api/lodgings/{id} | public | path | LodgingDTO | `200`, `404` |
| DELETE | /api/lodgings/{id} | admin +CSRF | path | none | `204`, `400`, `403`, `404` |
| GET | /api/lodgings/search | public | query | LodgingSearchResponse | `200`, `400` |
| GET | /api/lodgings/cities | public | query | `List<String>` | `200` |
| GET | /api/lodgings/{id}/availability | public | query | AvailabilityResponse | `200`, `404` |
| GET | /api/policies | public | none | `List<PolicyDTO>` | `200` |
| GET | /api/policies/{id} | public | path | PolicyDTO | `200`, `404` |
| POST | /api/policies | admin +CSRF | PolicyDTO | PolicyDTO | `201`, `400`, `403`, `409` |
| PUT | /api/policies/{id} | admin +CSRF | PolicyDTO | PolicyDTO | `200`, `400`, `403`, `404`, `409` |
| DELETE | /api/policies/{id} | admin +CSRF | path | none | `204`, `403`, `404` |
| POST | /api/ratings | authenticated +CSRF | RatingRequest | RatingDTO | `201`, `400`, `403`, `404` |
| GET | /api/ratings/lodging/{lodgingId} | public | path | Map | `200` |
| GET | /api/ratings/lodging/{lodgingId}/eligibility | authenticated | path | RatingEligibilityDTO | `200`, `403` |
| POST | /api/reservations | authenticated +CSRF | CreateReservationRequest | ReservationResponse | `201`, `400`, `401`, `404`, `409` |
| GET | /api/reservations/{id} | authenticated | path | ReservationResponse | `200`, `401`, `404` |
| PATCH | /api/reservations/{id}/cancel | authenticated +CSRF | path | ReservationResponse | `200`, `400`, `401`, `404`, `409` |
| GET | /api/reservations/my | authenticated | none | `List<ReservationResponse>` | `200`, `401` |
| GET | /api/reservations/admin | admin | query | `PageResponse<ReservationResponse>` | `200`, `400`, `401`, `403` |
| POST | /api/upload | admin +CSRF | multipart | UploadResult | `200`, `400`, `403`, `413`, `500` |
| GET | /api/users | admin | none | `List<UserDTO>` | `200`, `403` |
| PUT | /api/users/{id}/role | admin +CSRF | RoleRequest | UserDTO | `200`, `400`, `403`, `404`, `409` |
| PATCH | /api/users/{id}/enabled | admin +CSRF | UserStatusRequest | UserDTO | `200`, `403`, `404`, `409` |
| GET | /api/admin/stats | admin | none | AdminStatsResponse | `200`, `403` |

## Controller notes

### Auth (`/api/auth`)

- `POST /api/auth/login`, `POST /api/auth/register`: the `429` in their rows is a
  filter-level response (`AuthRateLimitFilter`), not the handler's own. The filter
  answers with a `Retry-After` header and the standard `{error, status}` body before the
  controller runs; the per-minute limits are separate for IP and for email account.
- `POST /api/auth/refresh`: the `429` is likewise filter-level (`RefreshRateLimitFilter`,
  IP-only fixed window, `Retry-After` header).
- `POST /api/auth/login`, `POST /api/auth/register` and `POST /api/auth/refresh` answer
  with `Set-Cookie`: login and register set `ACCESS_TOKEN` (and `REFRESH_TOKEN` when
  refresh sessions are enabled) and rotate the `XSRF-TOKEN` cookie; refresh rotates both
  session cookies. A rejected refresh (`401`) clears the now-useless `REFRESH_TOKEN`
  cookie.
- `POST /api/auth/logout` and `POST /api/auth/password` answer `204` with `Set-Cookie`
  headers that clear `ACCESS_TOKEN` and `REFRESH_TOKEN`.
- `POST /api/auth/welcome-email/resend` answers `202` when the email was scheduled,
  `200` with `{"status": "pending"}` when one is already scheduled, and `429` with a
  `Retry-After` header (seconds) while the resend cooldown is active. The `429` here is
  the handler's own, not a filter's.
- `GET /api/auth/csrf` materializes a readable `XSRF-TOKEN` cookie (`204`).

### Lodgings (`/api/lodgings`)

- `GET /api/lodgings` returns three shapes: with `?category=` a plain list, with both
  `?page=` and `?size=` a `PageResponse`, and without parameters the full list. There
  are no defaults: `page` and `size` only paginate when both are present.
- `GET /api/lodgings/search`: `page` defaults to 0 and `size` to 9. All filters are
  optional; `checkIn` and `checkOut` must be sent together and in order, and
  `minPrice` must not exceed `maxPrice` (violations answer `400`).
- `GET /api/lodgings/admin` and `GET /api/lodgings/recommendations`: `page` defaults to
  0; `size` defaults to 10. `size` is capped at 100 for the admin page and at 10 for
  recommendations; violations answer `400`.
- `GET /api/lodgings/recommendations` requires `seed` (16–64 characters of
  `[A-Za-z0-9_-]`); `revision` is optional.

### Reservations (`/api/reservations`)

- `GET /api/reservations/admin`: `page` defaults to 0 and `size` to 10 (max 100);
  `sort` defaults to `id`, `direction` to `asc`; `status` and `q` are optional filters.
  Invalid sorting values answer `400`.

### Categories, features, policies (`/api/categories`, `/api/features`, `/api/policies`)

- No pagination on any read endpoint.
- `DELETE /api/categories/{id}` answers `400` when the category still has lodgings.

### Ratings (`/api/ratings`)

- `POST /api/ratings` creates or updates the caller's rating for a lodging; only users
  with a completed confirmed reservation are eligible.
- `GET /api/ratings/lodging/{lodgingId}` returns ratings, average score and count for a
  lodging.

### Admin (`/api/admin`)

- `GET /api/admin/stats` returns database counts, not listings; no pagination applies.

## How to read the statuses

Which exception produces which status, and the exact JSON body of each error, is the
domain of [api-error-contract.md](api-error-contract.md) — see its mapping table. This
section only adds what that file does not cover:

- `DELETE /api/categories/{id}` answers `400` when the category is in use by at least
  one lodging (`LocalizedIllegalArgumentException` → `GlobalExceptionHandler`).
- `DELETE /api/lodgings/{id}` answers `400` when the lodging has reservations
  (`IllegalArgumentException` → `400`).
- `400` on `GET /api/lodgings/search`, `GET /api/lodgings/admin`,
  `GET /api/lodgings/recommendations` and `GET /api/reservations/admin` covers the
  pagination and filter constraints (`@Min` / `@Max` / `@Pattern` on query parameters →
  `ConstraintViolationException`), not just body validation.
- `403` with a missing or invalid CSRF header: the security filter chain answers `403`
  before any controller runs (`SecurityErrorHandler`, message `error.csrf_invalid`).
  CSRF checks run before role authorization, so an anonymous POST without a CSRF token
  to a protected path answers `403`, not `401`.
- `429` on `POST /api/auth/login` and `POST /api/auth/register` is the auth rate-limit
  filter's answer (see the Auth notes above); it carries a `Retry-After` header.
- `POST /api/ratings` lists `404` (lodging not found) but in practice cannot return it
  on the eligibility failure: the eligibility check runs first and answers `400`, and
  only a lodging deleted in the race between the two checks could still reach the `404`.

## What the generated spec adds

The OpenAPI spec served by this backend goes beyond this file:

- Every request/response schema (`ApiError`, the DTOs), with field types and required
  properties.
- Parameter constraints and defaults (`@Min`, `@Max`, `@Pattern`) per endpoint.
- The `csrfToken` security scheme (`X-XSRF-TOKEN` header) on unsafe endpoints.
- Descriptions and examples per operation.

Read it at `/v3/api-docs` and browse it with `/swagger-ui/index.html`. Both are
disabled in production (`springdoc.api-docs.enabled` and `springdoc.swagger-ui.enabled`
default to `false` in `application-prod.properties`).
