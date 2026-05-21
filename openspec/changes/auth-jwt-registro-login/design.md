# Design: Auth JWT — Registro, Login y Cierre de Sesión

## Technical Approach

Backend and frontend will share one stateless JWT contract. Spring Security will keep `/auth/**` and `/api/lodgings/**` public, then route everything else through a JWT filter that loads `User` and sets the security context. `AuthServiceImpl` will own register/login and return one response DTO with token + user data. React will centralize auth in `AuthContext`, persist the token in `localStorage`, decode claims with `jwt-decode`, and extend `api.js` to send `Authorization: Bearer <token>` and clear session on 401.

## Architecture Decisions

| Decision | Choice | Alternatives | Why |
|---|---|---|---|
| User model | `User` implements `UserDetails` | Adapter entity, separate principal | Fixed decision; simplest Spring Security integration. |
| Service boundary | `IAuthService` + `AuthServiceImpl` | Monolithic auth/user service | Leaves `IUserService` free for story #16. |
| JWT handling | `JwtService` + `JwtAuthenticationFilter` + `ApplicationConfig` | Session auth, controller-generated tokens | Reusable and consistent with current configuration package. |
| Logout | Client-only token/state removal | Backend blacklist endpoint | JWT is stateless and refresh tokens are out of scope. |
| Frontend integration | `AuthContext` + hook + fetch wrapper | Redux, axios interceptors | Keeps current React app lightweight. |

## Data Flow

| Flow | Steps |
|---|---|
| Register | `POST /auth/register` → validate DTO → check unique email → hash password → assign `USER` → build avatar URL → save user → issue JWT → `201`. |
| Login | `POST /auth/login` → `AuthenticationManager` validates credentials → service issues JWT with `sub`, `firstName`, `lastName`, `role`, `image` → frontend stores token and user. |
| Authenticated request | `api.js` adds Bearer header → `JwtAuthenticationFilter` validates token, loads `User`, sets `SecurityContext` → protected controller runs; bad token returns `401`. |

## File Changes

| File | Action | Description |
|---|---|---|
| `backend/pom.xml` | Modify | Add jjwt 0.12.6 dependencies. |
| `backend/src/main/resources/application.properties` | Modify | Add `app.jwt.secret` fallback config. |
| `backend/src/main/java/com/tuhospedaje/configuration/SecurityConfig.java` | Modify | Add JWT chain and final route rules. |
| `backend/src/main/java/com/tuhospedaje/configuration/ApplicationConfig.java` | Create | Expose auth beans. |
| `backend/src/main/java/com/tuhospedaje/configuration/JwtService.java` | Create | Generate and validate HS256 JWTs. |
| `backend/src/main/java/com/tuhospedaje/configuration/JwtAuthenticationFilter.java` | Create | Authenticate Bearer tokens before username/password filter. |
| `backend/src/main/java/com/tuhospedaje/entity/User.java` | Create | JPA entity implementing `UserDetails`. |
| `backend/src/main/java/com/tuhospedaje/enums/RoleEnum.java` | Create | USER / ADMIN roles. |
| `backend/src/main/java/com/tuhospedaje/repository/UserRepository.java` | Create | Email lookup. |
| `backend/src/main/java/com/tuhospedaje/dto/auth/*.java` | Create | Register, login, and auth response DTOs. |
| `backend/src/main/java/com/tuhospedaje/controller/AuthController.java` | Create | Auth endpoints. |
| `backend/src/main/java/com/tuhospedaje/service/IAuthService.java` | Create | Auth use-case contract. |
| `backend/src/main/java/com/tuhospedaje/service/impl/AuthServiceImpl.java` | Create | Register/login orchestration. |
| `backend/src/main/java/com/tuhospedaje/exception/GlobalExceptionHandler.java` | Modify | Map validation and auth errors. |
| `backend/src/test/java/com/tuhospedaje/auth/*Test.java` | Create | Auth coverage. |
| `frontend/package.json` | Modify | Add `jwt-decode`. |
| `frontend/src/services/api.js` | Modify | Add auth header injection and 401 handling. |
| `frontend/src/contexts/AuthContext.jsx` | Create | Store auth state. |
| `frontend/src/hooks/useAuth.js` | Create | Auth hook. |
| `frontend/src/pages/LoginPage.jsx` | Create | Login form page. |
| `frontend/src/pages/RegisterPage.jsx` | Create | Registration form page. |
| `frontend/src/components/Header/Header.jsx` | Modify | Switch between anonymous links and avatar/menu. |
| `frontend/src/App.jsx` | Modify | Add provider and auth routes. |
| `frontend/src/assets/css/auth.css` | Modify | Reuse auth styles for new pages/header state. |

## Interfaces / Contracts

| Contract | Shape |
|---|---|
| Register request | `firstName`, `lastName`, `email`, `password` |
| Login request | `email`, `password` |
| Auth response | `token`, `user { firstName, lastName, email, role, image }` |
| Endpoints | `POST /auth/register` → `201`, `POST /auth/login` → `200` |
| Error contract | `400` validation payload, `409` duplicate email, `401` invalid credentials/token |
| JWT claims | `sub=email`, `firstName`, `lastName`, `role`, `image`, `iat`, `exp` |

## Testing Strategy

| Layer | What to Test | Approach |
|---|---|---|
| Unit | JWT expiry, avatar URL, duplicate email, password hashing, login failures | JUnit 5 + Mockito. |
| Integration | `/auth/register`, `/auth/login`, public vs protected routes, invalid token behavior | `@SpringBootTest` + MockMvc. |
| Frontend | Token persistence, logout cleanup, header state | ESLint + manual smoke; no frontend runner yet. |

## Implementation Order

| Step | Depends on | Outcome |
|---|---|---|
| 1 | None | Add backend/frontend dependencies and JWT property. |
| 2 | 1 | Create `User`, `RoleEnum`, `UserRepository`, auth DTOs. |
| 3 | 2 | Implement `JwtService`, `ApplicationConfig`, `JwtAuthenticationFilter`. |
| 4 | 2-3 | Implement `IAuthService`, `AuthServiceImpl`, `AuthController`, exception mappings. |
| 5 | 3-4 | Update `SecurityConfig` and verify public/protected routes. |
| 6 | 4 | Add backend unit and integration tests under strict TDD. |
| 7 | 1,4-5 | Implement `AuthContext`, pages, header, routing, and authenticated fetch flow. |
