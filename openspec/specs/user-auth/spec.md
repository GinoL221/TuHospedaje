# User Authentication Specification

## Purpose

Registro de usuarios, login con JWT, cierre de sesión, protección de rutas por rol, y avatar generado por URL. Covers user stories #13, #14, #15.

## Requirements

### Requirement: User Registration

The system SHALL allow users to create an account with firstName, lastName, email, and password. On success, SHALL return a JWT token and created user data. Password SHALL be stored hashed.

#### Scenario: Successful registration

- GIVEN valid firstName, lastName, email (unique), and password meeting minimum length
- WHEN POST to registration endpoint
- THEN returns HTTP 201 with JWT token and user data (firstName, lastName, email, role=USER, image URL)

#### Scenario: Duplicate email

- GIVEN a user exists with email "test@example.com"
- WHEN POST with same email
- THEN returns HTTP 409 with "email already registered"

#### Scenario: Invalid fields

- GIVEN email with invalid format, empty firstName/lastName, or password below minimum length
- WHEN POST with invalid data
- THEN returns HTTP 400 with field-specific validation error

### Requirement: User Login

The system SHALL authenticate users with email and password. On success, SHALL issue a JWT with claims: sub (email), firstName, lastName, role, image. Token SHALL expire after 8 hours.

#### Scenario: Successful login

- GIVEN registered user with valid credentials
- WHEN POST with correct email and password
- THEN returns HTTP 200 with JWT containing claims: sub, firstName, lastName, role, image

#### Scenario: Invalid credentials

- GIVEN wrong password or non-existent email
- WHEN POST with those credentials
- THEN returns HTTP 401 with generic "invalid credentials" (same message for both cases)

#### Scenario: Avatar URL

- GIVEN user with firstName "Juan", lastName "Pérez"
- WHEN login succeeds
- THEN image field contains ui-avatars.com URL encoding initials "JP"

### Requirement: Logout

The system SHALL support logout via client-side state invalidation. Backend SHALL NOT require a logout endpoint (JWT is stateless).

#### Scenario: Successful logout

- GIVEN logged-in user with JWT in client storage
- WHEN user initiates logout
- THEN client removes JWT from storage and clears user state
- AND UI displays anonymous mode (login/register links)

### Requirement: Route Protection

The system SHALL enforce access control by route pattern. Public routes SHALL NOT require authentication. Protected routes SHALL require valid JWT in Authorization header.

| Route Pattern | Access | Description |
|---|---|---|
| `/auth/**` | Public | Registration and login endpoints |
| `/api/lodgings/**` | Public (Sprint 1 only) | Lodging CRUD — must be protected by end of Sprint 2 |
| All other routes | Protected | Require valid JWT |

#### Scenario: Public endpoint accessible without token

- GIVEN no authentication token
- WHEN request to `/auth/**` or `/api/lodgings/**`
- THEN request is processed without authentication

#### Scenario: Missing token on protected route

- GIVEN no token
- WHEN request to protected endpoint
- THEN returns HTTP 401 or 403

#### Scenario: Invalid token on protected route

- GIVEN expired or malformed JWT
- WHEN request to protected endpoint with Authorization header
- THEN returns HTTP 401

#### Scenario: Valid token on protected route

- GIVEN valid, non-expired JWT
- WHEN request to protected endpoint with `Bearer <token>` in Authorization header
- THEN request is processed with user identity from token claims

### Requirement: JWT Configuration

The system SHALL sign JWT tokens using HMAC-SHA256 (HS256). Secret key SHALL be configurable via `app.jwt.secret` property with environment variable `APP_JWT_SECRET` override.

#### Scenario: Secret from environment variable

- GIVEN `APP_JWT_SECRET` is set
- WHEN application starts
- THEN JWT service uses env var as signing key

#### Scenario: Secret from properties fallback

- GIVEN `APP_JWT_SECRET` not set and `app.jwt.secret` configured in properties
- WHEN application starts
- THEN JWT service uses properties value as signing key
