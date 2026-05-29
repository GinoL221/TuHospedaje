# Delta for Frontend Search

## ADDED Requirements

### Requirement: Home Search Form

The Home page SHALL display a search form with city autocomplete, date inputs, and a search button. The city input SHALL fetch suggestions from `/api/lodgings/cities?q=` with 300ms debounce.

#### Scenario: City autocomplete on typing

- GIVEN user is on Home page
- WHEN user types "bar" in city input
- THEN after 300ms debounce, fetches `/api/lodgings/cities?q=bar` and displays suggestions

#### Scenario: Select city from autocomplete

- GIVEN autocomplete shows ["Bariloche", "Buenos Aires"]
- WHEN user clicks "Bariloche"
- THEN city input is populated with "Bariloche" and dropdown closes

#### Scenario: Date validation before search

- GIVEN check-in date is after check-out date
- WHEN user clicks "Realizar búsqueda"
- THEN search is NOT triggered and user sees validation error

#### Scenario: Navigate to search results

- GIVEN valid city, check-in, and check-out are entered
- WHEN user clicks "Realizar búsqueda"
- THEN navigates to `/search?city=...&checkIn=...&checkOut=...`

### Requirement: Search Results Page

The system SHALL render a search results page at `/search` that reads query params, fetches results from `/api/lodgings/search`, and displays them with sidebar filters.

#### Scenario: Display search results

- GIVEN query params: city=Buenos Aires, checkIn=2026-07-01, checkOut=2026-07-05
- WHEN page loads
- THEN fetches search API and displays result cards (image, name, city, price/night, score)

#### Scenario: Sidebar filters refine results

- GIVEN search results are displayed
- WHEN user selects category filter and price range
- THEN results are re-fetched with additional filter params

#### Scenario: Empty results state

- GIVEN search returns no lodgings
- WHEN results page renders
- THEN displays empty state message ("No se encontraron resultados para tu búsqueda")

#### Scenario: URL is shareable

- GIVEN user performs a search
- WHEN user copies the URL
- THEN another user opening the URL sees the same search results

### Requirement: Search Endpoint is Public (Frontend)

The frontend SHALL call search, cities, and availability endpoints without requiring authentication headers.

#### Scenario: Search works for anonymous users

- GIVEN user is not logged in
- WHEN user performs a search from Home
- THEN search results load successfully without auth errors
