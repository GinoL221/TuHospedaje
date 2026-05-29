# Delta for Frontend Availability

## ADDED Requirements

### Requirement: Product Detail Price Display

The ProductDetail page SHALL display the lodging's price per night and calculate an estimated total based on selected date range.

#### Scenario: Show price per night

- GIVEN lodging has pricePerNight = 150.00
- WHEN user views ProductDetail
- THEN displays "$150.00 / noche"

#### Scenario: Calculate estimated total

- GIVEN pricePerNight = 100.00, user selects checkIn=2026-07-01, checkOut=2026-07-05
- THEN displays estimated total: "$400.00" (4 nights × $100)

### Requirement: Calendar with Occupied Dates

The ProductDetail page SHALL display a calendar (react-datepicker) that disables dates already reserved for the lodging.

#### Scenario: Occupied dates are disabled

- GIVEN lodging has reservations for 2026-07-10 to 2026-07-15
- WHEN calendar renders
- THEN dates 2026-07-10 through 2026-07-14 are disabled (not selectable)

#### Scenario: Available dates are selectable

- GIVEN no reservations for 2026-08-01 to 2026-08-10
- WHEN calendar renders
- THEN dates in that range are selectable

#### Scenario: Past dates are disabled

- WHEN calendar renders
- THEN all dates before today are disabled

### Requirement: Reserve Button for Authenticated Users

The ProductDetail page SHALL show a "Reservar" button only to authenticated users. Anonymous users SHALL see a prompt to log in.

#### Scenario: Authenticated user sees reserve button

- GIVEN user is logged in with valid JWT
- WHEN user views ProductDetail with dates selected
- THEN "Reservar" button is visible and clickable

#### Scenario: Anonymous user sees login prompt

- GIVEN user is not logged in
- WHEN user views ProductDetail
- THEN "Reservar" button is replaced with "Iniciá sesión para reservar" link

### Requirement: Reservation Confirmation Modal

The system SHALL display a confirmation modal before creating a reservation. The modal SHALL show lodging name, dates, guest info form, and total price.

#### Scenario: Submit reservation from modal

- GIVEN user is authenticated, dates selected, modal open
- WHEN user fills guest name, email and clicks "Confirmar"
- THEN POST `/api/reservations` is sent and on success, shows confirmation message

#### Scenario: Reservation conflict shown in modal

- GIVEN another user reserved the same dates between page load and confirmation
- WHEN user clicks "Confirmar"
- THEN modal shows error "Estas fechas ya no están disponibles"
