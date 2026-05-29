# Delta for Reservation Management

## ADDED Requirements

### Requirement: Create Reservation

The system SHALL allow authenticated users to create a reservation for a lodging. The system MUST validate no overlapping reservations exist and use optimistic locking (`@Version`) to prevent concurrent double-booking.

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| lodgingId | Long | Yes | Must reference existing lodging |
| checkIn | LocalDate | Yes | Must be in the future, before checkOut |
| checkOut | LocalDate | Yes | Must be after checkIn |
| guestName | String | Yes | Non-empty, max 100 chars |
| guestEmail | String | Yes | Valid email format |
| totalPrice | BigDecimal | Yes | Non-negative |

#### Scenario: Successful reservation creation

- GIVEN authenticated user, lodging #1 exists and is available for 2026-07-01 to 2026-07-05
- WHEN POST `/api/reservations` with valid body
- THEN returns HTTP 201 with reservation data including status=CONFIRMED

#### Scenario: Overlapping reservation rejected

- GIVEN lodging #1 has a reservation from 2026-07-02 to 2026-07-04
- WHEN POST `/api/reservations` with checkIn=2026-07-01, checkOut=2026-07-03
- THEN returns HTTP 409 with "lodging not available for selected dates"

#### Scenario: checkOut before checkIn rejected

- WHEN POST `/api/reservations` with checkIn=2026-07-05, checkOut=2026-07-01
- THEN returns HTTP 400 with validation error

#### Scenario: Past dates rejected

- WHEN POST `/api/reservations` with checkIn=2020-01-01
- THEN returns HTTP 400 with validation error

#### Scenario: Unauthenticated user cannot create reservation

- GIVEN no JWT token
- WHEN POST `/api/reservations` with valid body
- THEN returns HTTP 401 or 403

#### Scenario: Concurrent reservation attempt (optimistic locking)

- GIVEN two users simultaneously attempt to reserve lodging #1 for the same dates
- WHEN both POST requests arrive within the same transaction window
- THEN one succeeds (HTTP 201) and the other receives HTTP 409 (OptimisticLockException handled)

### Requirement: Get Reservation Detail

The system SHALL allow users to retrieve details of a specific reservation by ID.

#### Scenario: Get existing reservation

- GIVEN reservation #1 exists with status=CONFIRMED
- WHEN GET `/api/reservations/1`
- THEN returns HTTP 200 with full reservation data

#### Scenario: Get non-existent reservation

- WHEN GET `/api/reservations/999`
- THEN returns HTTP 404

### Requirement: Reservation Status

The system SHALL track reservation status as CONFIRMED or CANCELLED. New reservations SHALL default to CONFIRMED.

#### Scenario: New reservation has CONFIRMED status

- WHEN POST `/api/reservations` with valid data
- THEN created reservation has status=CONFIRMED

### Requirement: Optimistic Locking on Reservation

The Reservation entity SHALL include a `@Version` field (Long) to detect concurrent modifications. OptimisticLockException SHALL be handled in GlobalExceptionHandler returning HTTP 409.

#### Scenario: OptimisticLockException handled gracefully

- GIVEN a reservation is being updated concurrently by two requests
- WHEN the second request attempts to save with stale version
- THEN GlobalExceptionHandler catches OptimisticLockException and returns HTTP 409 with "reservation was modified by another user"
