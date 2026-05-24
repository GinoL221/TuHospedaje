# Delta for User Authentication

## MODIFIED Requirements

### Requirement: Route Protection

The system SHALL enforce access control by route pattern. Public routes SHALL NOT require authentication. Protected routes SHALL require valid JWT in Authorization header.

(Previously: Added `/api/categories/**` as a public route pattern)

| Route Pattern | Access | Description |
|---|---|---|
| `/auth/**` | Public | Registration and login endpoints |
| `/api/categories/**` | Public | Category listing and detail (GET), write operations require JWT |
| `/api/lodgings/**` | Public (Sprint 1 only) | Lodging CRUD — must be protected by end of Sprint 2 |
| All other routes | Protected | Require valid JWT |

#### Scenario: Public endpoint accessible without token

- GIVEN no authentication token
- WHEN request to `/auth/**`, `/api/categories/**` (GET only), or `/api/lodgings/**`
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
