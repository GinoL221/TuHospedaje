# Backend Architecture — Single Source of Truth

> This document describes the backend as it is built today in `backend/src/main/java/com/tuhospedaje`. Every claim cites `path:line` against the current code; nothing is derived from sprint reports or historical definitions. Line numbers may drift with future edits; the cited file is always authoritative.

Verified stack: Spring Boot 4.0.8 (`backend/pom.xml:7`), Jakarta Servlet API, Spring Security, Spring Data JPA on MariaDB, Flyway, Lombok.

## 1. Package layout

All components live under `com.tuhospedaje`, with `BackendApplication.java` (1 class) as the entry point.

| Package | Classes | Purpose |
| --- | ---: | --- |
| `configuration` | 24 | Application configuration: security chain, rate-limit filters, cookie factories, typed `*Properties` records, JWT, dev seeding and guards. |
| `controller` | 11 | REST controllers (`@RestController`), one per exposed resource (`/api/*`). |
| `dto` | 28 | Transfer objects grouped by domain (`auth`, `lodging`, `reservation`, `email`, `common`), including `ApiError` and `PageResponse`. |
| `entity` | 13 | JPA entities: `User`, `Lodging`, `Reservation`, `Category`, `Feature`, `Policy`, `Rating`, `RefreshToken`, `RefreshTokenFamily`, `EmailOutbox`, `SessionSecurityEvent`, `LodgingImage`, `AdminInvariantLock`. |
| `enums` | 4 | Domain enums: `EmailOutboxStatus`, `EmailOutboxType`, `ReservationStatus`, `RoleEnum`. |
| `exception` | 9 | Domain exceptions with HTTP status semantics plus `GlobalExceptionHandler`. |
| `openapi` | 1 | `OpenApiConfig`: metadata for Swagger/OpenAPI. |
| `repository` | 16 | Spring Data repositories (including `EmailOutboxClaimRepository` with its JDBC implementation `EmailOutboxRepositoryImpl`, the `CityProjection` interface, and `specification/ReservationSpecifications`). |
| `security` | 2 | `FixedWindowRateLimiter` (reusable fixed-window counter) and `RefreshTokenHasher` (HMAC hashing of refresh credentials). |
| `service` | 20 + 1 | Service interfaces per domain, terminal collaborators (`EmailTransport`, `EmailTemplateRenderer`, `ImageContentValidator`, email renderers), and a `command` subpackage with `UploadImageCommand`. |
| `service/impl` | 18 | Service implementations and orchestration beans (dispatcher, scheduler, outbox transaction service, registration persistence, local image storage). |
| `validation` | 2 | `HttpsImageUrl` + `HttpsImageUrlValidator`: Bean Validation for HTTPS image URLs. |

## 2. Actual request flow

The chain is built in `SecurityConfig.securityFilterChain` (`configuration/SecurityConfig.java:44-139`).

**Filter order.** Three filters are registered before the `UsernamePasswordAuthenticationFilter` anchor, in this order (`SecurityConfig.java:135-137`):

1. `authRateLimitFilter` — per-IP and per-email ceiling for `POST /api/auth/login` and `POST /api/auth/register` (`configuration/AuthRateLimitFilter.java:118`).
2. `refreshRateLimitFilter` — per-IP-only ceiling for `POST /api/auth/refresh` (`configuration/RefreshRateLimitFilter.java:49`).
3. `jwtAuthenticationFilter` — authentication from the HTTP-only `ACCESS_TOKEN` cookie (`configuration/JwtAuthenticationFilter.java:26-79`).

Both rate-limit filters run before the JWT filter, so a throttled request never reaches credential validation or the database (`RefreshRateLimitFilter.java:120-121` states this explicitly: "short-circuit: DB never touched").

**CSRF.** `CookieCsrfTokenRepository.withHttpOnlyFalse()` stores the token in the `XSRF-TOKEN` cookie (`SecurityConfig.java:37-40`). The request handler is `SpaCsrfTokenRequestHandler`, which resolves both the plain `X-XSRF-TOKEN` header value and Spring's Xor token-parameter format (`configuration/SpaCsrfTokenRequestHandler.java:16-41`). `POST /api/auth/login`, `/api/auth/register` and `/api/auth/refresh` are CSRF-exempt because they are unauthenticated-context entry points authenticated by their own credential (`ignoringRequestMatchers` and design comment, `SecurityConfig.java:56-59`); `/api/auth/welcome-email/resend` is exempt only when the request carries no `ACCESS_TOKEN` cookie (`SecurityConfig.java:57-59`). With `SessionCreationPolicy.STATELESS` (`SecurityConfig.java:134`) there is no session to fixate, so the session authentication strategy is `NullAuthenticatedSessionStrategy`, removing the CSRF-cookie rotation race against concurrent post-login requests (`SecurityConfig.java:69-72`).

**SecurityErrorHandler and the security boundary.** Security errors are answered before MVC; `@RestControllerAdvice` never sees them. `configuration/SecurityErrorHandler.java:30-64` implements `AccessDeniedHandler` and exposes the `entryPoint(HttpStatus)` factory; `SecurityConfig.java:107-133` wires it through `defaultAuthenticationEntryPointFor`: 401 for unauthenticated requests to `/api/reservations/**`, `/api/auth/me`, `/api/auth/welcome-email/resend` and `/api/auth/csrf`; 403 for everything else; a 403 with a specific CSRF message when the token is missing or expired (`SecurityErrorHandler.java:45-54`). Every answer uses the same `{error, status}` JSON body as `GlobalExceptionHandler` (comment at `SecurityConfig.java:102-104`).

## 3. Layering as actually practised

The dominant pattern is `Controller → Service interface → ServiceImpl → Repository → Entity/DTO`: each domain has its interface (`service/UserService.java`, `service/ReservationService.java`, etc.) and its `*ServiceImpl` in `service/impl` (`service/impl/UserServiceImpl.java:24`, `service/impl/ReservationServiceImpl.java:39`, and likewise for Auth, AdminStats, Category, Feature, Lodging, LodgingQuery, Policy, Rating, EmailOutbox and RefreshSession). `SmtpEmailServiceImpl` implements the `EmailTransport` collaborator and `LocalImageStorageService` implements `ImageStorageService` (`service/impl/SmtpEmailServiceImpl.java:22`, `service/impl/LocalImageStorageService.java:37`).

Exactly four beans in `service/impl` have **no interface**:

| Class | Role |
| --- | --- |
| `EmailOutboxTransactionService` (`service/impl/EmailOutboxTransactionService.java:21-22`) | Owns the outbox transactional boundaries: the claim batch and every outcome write in `REQUIRES_NEW`. It is a transactional boundary, not a domain operation. |
| `EmailOutboxDispatcher` (`service/impl/EmailOutboxDispatcher.java:16-18`) | Runs the claim → transport → outcome cycle. It has a single possible implementation and is consumed via `ObjectProvider` (`service/impl/EmailOutboxScheduler.java:17`); an interface would buy nothing. |
| `EmailOutboxScheduler` (`service/impl/EmailOutboxScheduler.java:12-13`) | Scheduling infrastructure (`@Scheduled`), not business logic. |
| `RegistrationPersistenceService` (`service/impl/RegistrationPersistenceService.java:9-10`) | Extracts the transactional core of registration (persist the user + enqueue the welcome email) so that `AuthServiceImpl` orchestrates without hand-rolling `@Transactional`; it is a coordinating persistence unit. |

The working rule: domain services keep an interface so they can be tested and substituted in isolation; transactional-coordination and infrastructure beans are concrete classes because their contract is internal to the process.

## 4. Three locking strategies

**Optimistic (`@Version`).** `Reservation.java:63`, `Lodging.java:79` and `RefreshTokenFamily.java:68` carry `@Version` columns: two concurrent writes to the same row produce `ObjectOptimisticLockingFailureException`, translated to 409 by `GlobalExceptionHandler` (`exception/GlobalExceptionHandler.java:226-230`).

**Pessimistic write.** Three queries use `@Lock(LockModeType.PESSIMISTIC_WRITE)`:

- `LodgingRepository.findByIdForUpdate` (`repository/LodgingRepository.java:40-42`).
- `ReservationRepository.lockByLodgingIdAndStatus` and `findByIdForUpdate` (`repository/ReservationRepository.java:34-37,52-54`).
- `RefreshTokenFamilyRepository.findByIdForUpdate` (`repository/RefreshTokenFamilyRepository.java:17-19`).

`createReservation` locks the **parent lodging row**, not the reservations: `ReservationServiceImpl.java:93-98` explains why — it serializes concurrent reservation attempts for the same lodging even when zero reservations exist (locking reservations would match no rows and would not prevent the first double booking). The repository comment adds that InnoDB gap-locks the indexed `lodging_id` range, serializing inserts even when the result set is empty (`ReservationRepository.java:29-33`). The order at `ReservationServiceImpl.java:97,106-110`: `findByIdForUpdate` on the lodging, then `lockByLodgingIdAndStatus` over CONFIRMED reservations for the overlap check.

**Read paths deliberately stay lock-free.** Reads never touch the pessimistic queries: search and `checkAvailability` use the non-locking `findByLodgingIdAndStatus` (`ReservationServiceImpl.java:105`; `LodgingQueryServiceImpl.java:242`), and the history, admin and detail queries read under `@Transactional(readOnly = true)` without locks (`ReservationServiceImpl.java:143-220`). The cost of serializing reads against reservation writes is only paid inside `createReservation` and `cancelReservation` (`ReservationServiceImpl.java:62`, where the lock targets the reservation row being cancelled).

## 5. Refresh token families

The logic lives in `RefreshSessionServiceImpl` (`service/impl/RefreshSessionServiceImpl.java:27`), conditionally registered (`configuration/RefreshSessionConfiguration.java`) and fully switchable off via its kill switch (`RefreshSessionServiceImpl.java:38-43`).

- **Generation counter**: a family starts at `currentGeneration = 0` in `issue()` (`RefreshSessionServiceImpl.java:84`), and `rotate()` increments by 1 per rotation, persisting the successor token with its generation and predecessor (`RefreshSessionServiceImpl.java:127-136`).
- **Reuse detection runs before the family rate ceiling**: in `rotate()` the reuse branch comes first. If the presented token is already consumed, either the idempotent retry window applies or the family is revoked with reason `REUSE` and a `SessionSecurityEvent` of type `REFRESH_REUSE` is persisted (`RefreshSessionServiceImpl.java:99-107,238-258`). The per-family ceiling (`refreshPerFamilyPerMinute`) is applied **after** eligibility and the reuse branch: the comment at `RefreshSessionServiceImpl.java:112-120` is explicit — throttling must never mask a theft signal, and an ineligible credential must get its ordinary 401 rather than a 429, so it cannot consume family quota or leak the family's existence.
- **Idempotent retry window**: a consumed predecessor whose successor exists and is alive, re-presented within `retryGrace` of its `consumedAt`, is answered with the successor's credential without a new rotation (`isEligibleRetry`, `RefreshSessionServiceImpl.java:100-104,225-236`; the property lives in `configuration/SessionProperties.java:21`).
- **Per-family fixed-window key**: `familyLimiter` is a private `security/FixedWindowRateLimiter` (`RefreshSessionServiceImpl.java:43`) keyed by `"family:" + family.getId()` (`RefreshSessionServiceImpl.java:121`). The fixed-window algorithm is the shared one (`security/FixedWindowRateLimiter.java:35-43`).
- Token and family are pessimistically locked before any decision (`lockedToken`, `RefreshSessionServiceImpl.java:186-199`) to serialize concurrent rotations with the same credential.

## 6. Email outbox

- **Claim by lease token**: `claimBatch` generates a random UUID lease token and `leaseUntil = now + leaseDuration` (5 minutes by default, `configuration/EmailOutboxProperties.java:31`), then claims the batch with both values (`service/impl/EmailOutboxTransactionService.java:36-50`). The SQL (`repository/EmailOutboxRepositoryImpl.java:16-63`) selects rows with `FOR UPDATE SKIP LOCKED` — rows that are `PENDING` (or `PROCESSING` with an expired lease) — and flips them to `PROCESSING` with the token and `lease_until`. The `@Transactional` on `claimBatch` commits the claim on its own.
- **Every outcome written in `REQUIRES_NEW`**: `markDelivered`, `releaseForRetry` and `markFailed` each open their own transaction (`EmailOutboxTransactionService.java:57-72`). Because the claim is already committed, a dispatcher crash between the SMTP submit and the outcome write cannot roll the claim back; the row stays `PROCESSING` until its lease expires and a later cycle re-claims it — hence the by-design at-least-once semantics with possible duplicates after a crash.
- **Ownership via lease token**: every outcome updates by `id + lease_token` (`markDelivered(id, token, ...)` etc.); an update count of 0 means the lease changed hands and the result is discarded with a `stale_owner_rejected` log (`service/impl/EmailOutboxDispatcher.java:144-147,160-177,180-183`).
- **Backoff**: a per-attempt list, by default 1 m / 5 m / 15 m / 1 h (`EmailOutboxProperties.java:37-41`; active values at `backend/src/main/resources/application.properties:159-162`), applied on `releaseForRetry` as `nextAttemptAt = outcomeTime + backoff[attempt - 1]` (`EmailOutboxDispatcher.java:170-171`). Beyond `maxAttempts` (5 by default, `EmailOutboxProperties.java:34`) or on a non-retryable failure, the row moves to `FAILED` (`EmailOutboxDispatcher.java:156-168`).
- **Cycle and cleanup**: `EmailOutboxScheduler` polls with a `fixedDelay` of `tuhospedaje.email-outbox.poll-interval` over every `EmailOutboxType`, and cleans up completed rows past retention with a second `fixedDelay` (`service/impl/EmailOutboxScheduler.java:25-49`; 30-day retention, `EmailOutboxProperties.java:44,74-82`). The whole subsystem activates with `tuhospedaje.email-outbox.enabled=true` (`EmailOutboxScheduler.java:12`), and the dispatcher bean exists only when an `EmailTransport` bean exists (`EmailOutboxDispatcher.java:9-10`).

Active outbox backoff values are configured in `backend/src/main/resources/application.properties:159-162`.

## 7. Rate limiting

There are **two independent filter limiters** (plus the per-family ceiling from section 5, which lives in the service):

1. `AuthRateLimitFilter` — only `POST /api/auth/login` and `POST /api/auth/register` (`configuration/AuthRateLimitFilter.java:154-156`). It evaluates **two dimensions per request**, and both always count so that a blocked dimension still consumes the other's quota: `login|register:ip:<remoteAddr>` (`AuthRateLimitFilter.java:164-170`) and `login|register:email:<email>` with the email normalized to lowercase and trimmed (`AuthRateLimitFilter.java:161-172,188-200`). Either dimension exceeding the limit answers 429 with `Retry-After` (`AuthRateLimitFilter.java:174-177,290-299`).
2. `RefreshRateLimitFilter` — only `POST /api/auth/refresh`, single key `refresh:ip:<remoteAddr>` against `refreshPerIpPerMinute` (`configuration/RefreshRateLimitFilter.java:74,117-121`), never touching the body (the credential lives in a cookie, `RefreshRateLimitFilter.java:43-45`).

Shared details:

- **8192-byte body cap**: `MAX_BODY_BYTES` (`AuthRateLimitFilter.java:109`). An empty, malformed or oversized body never produces a 4xx from the filter: `extractEmailKey` returns `null` and the request degrades to IP-only keying; the downstream controller still answers with its normal 400 by re-reading the buffered body (`AuthRateLimitFilter.java:182-200`). The body is wrapped in a hand-rolled re-readable wrapper rather than `ContentCachingRequestWrapper` (`AuthRateLimitFilter.java:203-215`).
- **Fixed window with lazy sweep**: per-minute counters in a `ConcurrentHashMap`, swept inline when the map exceeds `MAX_TRACKED_KEYS = 10_000`, guarded by an `AtomicBoolean` so exactly one thread sweeps (`AuthRateLimitFilter.java:171,280-288`; the same algorithm in `security/FixedWindowRateLimiter.java:23-58`, consumed by `RefreshRateLimitFilter` and the family ceiling).
- **Matching hardening**: a case-insensitive path parser, a matcher accepting both the bare and trailing-slash patterns, and collapsing of repeated slashes before matching, to close casing and `//` bypasses (`AuthRateLimitFilter.java:132-147,192-199`; duplicated in `RefreshRateLimitFilter.java:51-93` by recorded design decision, `RefreshRateLimitFilter.java:36-41`).
- Kill switches: both filters always exist as beans and disable through properties alone in `shouldNotFilter` (`AuthRateLimitFilter.java:187-190`, `RefreshRateLimitFilter.java:81-84`).

## 8. Error handling

**A single `GlobalExceptionHandler`** (`@RestControllerAdvice`, `exception/GlobalExceptionHandler.java:34-35`) is the exception-to-status translation point for everything that reaches MVC. The body is always `{error, status}`, with a stable `code` field in two cases for the frontend to discriminate on.

| Exception | Status | Evidence (`GlobalExceptionHandler.java`) |
| --- | --- | --- |
| `ResourceNotFoundException` | 404 | :53-63 |
| `NoResourceFoundException` (static resources) | 404 | :275-283 |
| `DuplicateEmailException` | 409 + `code: duplicate_email` | :81-88 |
| `DuplicateResourceException` | 409 | :97-103 |
| `ReservationConflictException` | 409 | :122-132 |
| `IllegalArgumentException` (+ `LocalizedIllegalArgumentException`) | 400 | :134-152 |
| `ConstraintViolationException` | 400 | :166-173 |
| `HandlerMethodValidationException` | 400 | :183-191 |
| `MethodArgumentNotValidException` | 400 + `fields` | :216-224 |
| `AuthenticationException` | 401 | :210-214 |
| `ObjectOptimisticLockingFailureException` | 409 | :226-230 |
| `LastEnabledAdminException` | 409 + `code` | :232-239 |
| `PessimisticLockingFailureException` | 409 | :241-246 |
| `MaxUploadSizeExceededException` | 413 | :255-260 |
| `HttpMessageNotReadableException` | 400 | :291-296 |
| `HttpRequestMethodNotSupportedException` | 405 | :298-303 |
| `HttpMediaTypeNotSupportedException` | 415 | :305-310 |
| `MissingServletRequestParameterException` | 400 | :312-317 |
| `UploadException` | 500 | :319-324 |
| `DataIntegrityViolationException` | 400 | :326-330 |
| `MethodArgumentTypeMismatchException` | 400 | :332-336 |
| `Exception` (catch-all) | 500; re-throws `AccessDeniedException` toward Spring Security | :343-351 |

The two message paths:

1. **Fixed key via `MessageSource`**: most handlers resolve a fixed key (`error.rate_limit`, `error.auth.invalid`, `error.optimistic_lock`, etc., at the line ranges cited above).
2. **Literal with fallback**: for `IllegalArgumentException` and `ReservationConflictException` the exception message itself is tried as a key and, on `NoSuchMessageException`, the raw text is returned (documented comment at :65-73, implementation at :122-132,144-149) — today those throw sites use Spanish literals, not keys.

The active message catalog lives in **two bundles**: `backend/src/main/resources/messages.properties` (neutral/English) and `messages_es.properties` (Spanish), selected by `Accept-Language`. `spring.messages.fallback-to-system-locale=false` (`backend/src/main/resources/application.properties:99-106`) keeps resolution deterministic regardless of the host OS locale (with the rationale in the comment at :101-105). Security errors that never reach MVC — the boundary 401/403 — share the same body shape via `SecurityErrorHandler` with its own keys (`error.auth.required`, `error.access_denied`, `error.csrf_invalid`, `configuration/SecurityErrorHandler.java:33-36`).

## 9. What this document does not claim

- The package counts in section 1 reflect the current tree; adding classes requires updating them.
- Line numbers are exact as of the revision this document was written against and may shift; when in doubt, the cited file wins.
