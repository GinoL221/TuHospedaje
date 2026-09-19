# Reservation Identity Authority

## Goal

Make the authenticated account the authority for reservation guest identity. Persist `guestName` and `guestEmail` as historical snapshots derived by the backend.

## Scope

- Backend request/service create-reservation path
- Tests that construct `CreateReservationRequest` or POST identity fields
- BookingPage POST payload (no `guestName`/`guestEmail`)

## Non-goals

- Hexagonal migration
- Removing JPA `User` from `ReservationService`
- Email/upload refactors
- Booking for another person

## Tasks

- [x] 1. Backend derives snapshot identity on create.
  - Removed `guestName`/`guestEmail` from `CreateReservationRequest`.
  - `ReservationServiceImpl.createReservation` snapshots `User.firstName` + `lastName` and `User.email`.
  - `guestPhone` and `notes` remain request-owned.
  - Unit test `createReservation_snapshotsAuthenticatedUserIdentityInsteadOfClientPayload` and controller test posting spoofed JSON both persist/return authenticated identity.
  - Focused Maven tests passed: ReservationServiceImplTest, ReservationControllerIntegrationTest, ReservationConcurrencyTest, EmailOutboxEnqueueIntegrationTest, EmailOutboxEnqueueRollbackIntegrationTest.

## Work-unit evidence

Frontend POST no longer sends `guestName`/`guestEmail`. `BookingPage.test.jsx`: 33 passed. Commits pending.
