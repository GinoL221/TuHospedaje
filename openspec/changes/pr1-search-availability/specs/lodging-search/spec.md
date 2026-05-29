# Delta for Lodging Search

## ADDED Requirements

### Requirement: Unified Search Endpoint

The system SHALL provide a unified search endpoint that filters lodgings by city, date range, guest count, category, and price range. Lodgings with overlapping reservations in the requested date range SHALL be excluded.

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| city | String | No | City name (partial, case-insensitive) |
| checkIn | LocalDate | No | Check-in date (ISO) |
| checkOut | LocalDate | No | Check-out date (ISO) |
| guests | Integer | No | Min guest capacity |
| category | Long | No | Category ID |
| minPrice | BigDecimal | No | Min price/night |
| maxPrice | BigDecimal | No | Max price/night |

#### Scenario: Search by city only

- GIVEN lodgings in "Buenos Aires" and "Córdoba"
- WHEN GET `/api/lodgings/search?city=buenos`
- THEN returns matching lodgings (case-insensitive)

#### Scenario: Date overlap excludes reserved lodging

- GIVEN lodging #1 has reservation 2026-06-10 to 2026-06-15
- WHEN GET `/api/lodgings/search?checkIn=2026-06-12&checkOut=2026-06-14`
- THEN lodging #1 NOT in results

#### Scenario: Available lodging included

- GIVEN lodging #2 has no reservations
- WHEN GET with same date range
- THEN lodging #2 IS in results

#### Scenario: Price and guest filters

- GIVEN lodgings with pricePerNight: 50, 100, 200 and maxGuests: 2, 6
- WHEN GET `?minPrice=60&maxPrice=150&guests=4`
- THEN returns only lodging with price 100 and maxGuests >= 4

#### Scenario: No params returns all

- WHEN GET `/api/lodgings/search` with no params
- THEN returns all lodgings

### Requirement: City Autocomplete

The system SHALL return distinct city names matching a partial query, sorted alphabetically.

#### Scenario: Partial match returns cities

- GIVEN cities: "Buenos Aires", "Bariloche", "Córdoba"
- WHEN GET `/api/lodgings/cities?q=bar`
- THEN returns ["Bariloche", "Buenos Aires"]

#### Scenario: Empty query returns all cities

- WHEN GET `/api/lodgings/cities?q=`
- THEN returns all distinct cities sorted

### Requirement: Lodging Price and Capacity Fields

The system SHALL store `pricePerNight` (BigDecimal, >= 0) and `maxGuests` (Integer, > 0) on each lodging.

#### Scenario: Valid price and capacity accepted

- WHEN POST `/api/lodgings` with valid pricePerNight and maxGuests
- THEN values are persisted

#### Scenario: Negative price rejected

- WHEN POST with pricePerNight < 0
- THEN returns HTTP 400

### Requirement: Availability Check per Lodging

The system SHALL indicate if a specific lodging is available for a given date range.

#### Scenario: Available lodging

- GIVEN no overlapping reservations for the dates
- WHEN GET `/api/lodgings/1/availability?checkIn=2026-07-01&checkOut=2026-07-05`
- THEN returns `{ "available": true }`

#### Scenario: Occupied lodging

- GIVEN reservation overlaps the dates
- WHEN GET same endpoint
- THEN returns `{ "available": false }`

#### Scenario: Missing dates rejected

- WHEN GET without checkIn or checkOut
- THEN returns HTTP 400

### Requirement: Search Endpoints are Public

Search, cities, and availability endpoints SHALL NOT require authentication.

#### Scenario: Search without token

- GIVEN no JWT
- WHEN GET `/api/lodgings/search?city=buenos`
- THEN returns HTTP 200

## MODIFIED Requirements

### Requirement: Route Protection

(Previously: `/api/lodgings/**` was public in Sprint 1; new sub-routes listed explicitly)

| Route Pattern | Access |
|---|---|
| `/api/lodgings/search` | Public |
| `/api/lodgings/cities` | Public |
| `/api/lodgings/{id}/availability` | Public |
| `/api/lodgings/**` | Public (Sprint 1 only; protect by end of Sprint 2) |

#### Scenario: Availability without token

- GIVEN no auth token
- WHEN GET `/api/lodgings/1/availability?checkIn=2026-06-01&checkOut=2026-06-05`
- THEN request proceeds without authentication
