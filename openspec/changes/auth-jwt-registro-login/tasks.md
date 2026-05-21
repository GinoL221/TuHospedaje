# Tasks: Auth JWT — Registro, Login y Cierre de Sesión

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 900–1400 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR1 Backend base + tests core → PR2 Security + tests integración → PR3 Frontend auth flow |
| Delivery strategy | ask-always |
| Chain strategy | feature-branch-chain |

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: feature-branch-chain
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Base branch | Notes |
|------|------|-----------|-------------|-------|
| 1 | Modelo auth + service JWT + tests unitarios | PR 1 | sprint-2 | Base técnica backend, sin UI |
| 2 | Controller, SecurityConfig, errores + tests integración | PR 2 | rama PR1 | Depende PR1; cierra contrato HTTP |
| 3 | Contexto auth frontend + páginas + header + wiring | PR 3 | rama PR2 | Depende PR2; smoke manual |

## Phase 1: Foundation & Dependencies

- [ ] 1.1 Modificar `backend/pom.xml` agregando `jjwt-api`, `jjwt-impl`, `jjwt-jackson` (0.12.6) y validar `mvn -q -DskipTests compile`.
- [ ] 1.2 Modificar `backend/src/main/resources/application.properties` agregando `app.jwt.secret` fallback y expiración 8h.
- [ ] 1.3 Modificar `frontend/package.json` agregando `jwt-decode` y validar `npm install` sin conflictos.
- [ ] 1.4 Crear `backend/src/main/java/com/tuhospedaje/enums/RoleEnum.java` con `USER`, `ADMIN`.
- [ ] 1.5 Crear `backend/src/main/java/com/tuhospedaje/entity/User.java` (JPA + `UserDetails`) con campos y mapping de rol.
- [ ] 1.6 Crear `backend/src/main/java/com/tuhospedaje/repository/UserRepository.java` con `findByEmail`.
- [ ] 1.7 Crear DTOs en `backend/src/main/java/com/tuhospedaje/dto/auth/` (`RegisterRequest`, `LoginRequest`, `AuthResponse`) con validaciones.

## Phase 2: Backend Core (Strict TDD)

- [ ] 2.1 **RED** Crear `backend/src/test/java/com/tuhospedaje/auth/JwtServiceTest.java` cubriendo claims obligatorios, expiración y secret config.
- [ ] 2.2 **GREEN** Crear `backend/src/main/java/com/tuhospedaje/configuration/JwtService.java` para firmar/validar HS256 hasta pasar 2.1.
- [ ] 2.3 **RED** Crear tests unitarios de registro/login (duplicado, hash, credenciales inválidas, avatar URL) en `backend/src/test/java/com/tuhospedaje/auth/AuthServiceImplTest.java`.
- [ ] 2.4 **GREEN** Crear `backend/src/main/java/com/tuhospedaje/service/IAuthService.java` y `.../service/impl/AuthServiceImpl.java` para pasar 2.3.
- [ ] 2.5 Crear `backend/src/main/java/com/tuhospedaje/configuration/ApplicationConfig.java` (beans `PasswordEncoder`, `AuthenticationManager`, `UserDetailsService`).
- [ ] 2.6 Crear `backend/src/main/java/com/tuhospedaje/configuration/JwtAuthenticationFilter.java` para extraer Bearer y poblar `SecurityContext`.

## Phase 3: API & Security Wiring (Strict TDD)

- [ ] 3.1 **RED** Crear `backend/src/test/java/com/tuhospedaje/auth/AuthControllerIntegrationTest.java` con escenarios spec: register 201, login 200, 409, 400, 401, token inválido.
- [ ] 3.2 **GREEN** Crear `backend/src/main/java/com/tuhospedaje/controller/AuthController.java` (`POST /auth/register`, `POST /auth/login`) para pasar 3.1.
- [ ] 3.3 Modificar `backend/src/main/java/com/tuhospedaje/exception/GlobalExceptionHandler.java` mapeando 400/409/401 según contrato.
- [ ] 3.4 Modificar `backend/src/main/java/com/tuhospedaje/configuration/SecurityConfig.java` para dejar públicos `/auth/**`, `/api/lodgings/**` y proteger el resto.
- [ ] 3.5 **REFACTOR** Consolidar tests auth (`mvn test`) y eliminar duplicaciones manteniendo cobertura de escenarios.

## Phase 4: Frontend Auth Flow

- [ ] 4.1 Crear `frontend/src/contexts/AuthContext.jsx` con `login`, `register`, `logout`, persistencia en `localStorage` y decodificación JWT.
- [ ] 4.2 Crear `frontend/src/hooks/useAuth.js` para acceso tipificado al contexto.
- [ ] 4.3 Crear `frontend/src/pages/LoginPage.jsx`, `frontend/src/pages/RegisterPage.jsx` y estilos en `frontend/src/assets/css/auth.css`.
- [ ] 4.4 Modificar `frontend/src/services/api.js` para inyectar `Authorization` y limpiar sesión al recibir 401.
- [ ] 4.5 Modificar `frontend/src/components/Header/Header.jsx` para modo anónimo vs autenticado (nombre + avatar).
- [ ] 4.6 Modificar `frontend/src/App.jsx` integrando `AuthProvider`, rutas login/register y protección de navegación.

## Phase 5: Verification & Done Criteria

- [ ] 5.1 Ejecutar `mvn test` validando todos los escenarios backend de `openspec/specs/user-auth/spec.md`.
- [ ] 5.2 Ejecutar `npm run lint` y smoke manual: registro, login, logout, header condicional y 401 handling.
- [ ] 5.3 Verificar que `/api/lodgings/**` sigue público y documentar deuda de cierre Sprint 2 en comentario técnico.
