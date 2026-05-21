# Proposal: Auth JWT — Registro, Login y Cierre de Sesión

## Intent

Habilitar autenticación de usuarios (registro + login con JWT) para cubrir las user stories #13, #14 y #15 del Sprint 2. Sin auth, el sistema no puede identificar usuarios ni proteger rutas, bloqueando todas las funcionalidades futuras que requieren roles (admin, reservas, etc.).

## Scope

### In Scope
- **Backend**: Entity `User`, `RoleEnum`, `UserRepository`, `IAuthService` + `AuthServiceImpl`, JWT (`JwtService`, `JwtAuthenticationFilter`, `ApplicationConfig`), `AuthController` con DTOs, `SecurityConfig` actualizado, `pom.xml` con jjwt
- **Frontend**: `AuthContext.jsx` + `useAuth.js`, `LoginPage.jsx`, `RegisterPage.jsx`, `Header.jsx` condicional, `api.js` con token, `App.jsx` con rutas y provider
- **Testing backend**: Tests unitarios e integración para auth (strict TDD)
- **Avatar**: ui-avatars.com por URL (sin subida de imagen)

### Out of Scope
- #16 (asignar/quitar admin)
- #17, #18 (CRUD y visualización de características)
- #12, #20, #21 (categorías)
- #19 (email de bienvenida)
- Subida de foto de perfil
- OAuth / login social
- Refresh tokens

## Capabilities

### New Capabilities
- `user-auth`: Registro de usuarios, login con JWT, cierre de sesión, protección de rutas por rol, avatar generado por URL

### Modified Capabilities
- `None` — no existen specs previas en `openspec/specs/`

## Approach

**Backend**: Spring Security chain con `JwtAuthenticationFilter` antes del `UsernamePasswordAuthenticationFilter`. `User` implementa `UserDetails` directamente. `BCryptPasswordEncoder` via `ApplicationConfig`. JWT firmado con HMAC-SHA256 (jjwt 0.12.6), expiración 8h configurable via properties. Secret key por `@Value("${app.jwt.secret}")` con fallback en `application.properties` + variable de entorno `APP_JWT_SECRET` (Spring Boot la resuelve automáticamente). `/auth/**` público, `/api/lodgings/**` público (Sprint 1), resto autenticado gradualmente.

**Frontend**: `AuthContext` maneja estado (user, token, login, logout, register). Token en `localStorage`. `api.js` intercepta requests inyectando `Authorization: Bearer <token>` y redirige a login en 401. Header muestra avatar con iniciales + nombre o links de login/registro.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `backend/pom.xml` | Modified | Agregar jjwt 0.12.6 dependencies |
| `backend/.../configuration/SecurityConfig.java` | Modified | Incluir JWT filter, `/auth/**` público |
| `backend/.../entity/User.java` | New | Entity + UserDetails |
| `backend/.../enums/RoleEnum.java` | New | USER, ADMIN |
| `backend/.../repository/UserRepository.java` | New | findByEmail |
| `backend/.../auth/` | New | Controller + 3 DTOs |
| `backend/.../security/` | New | JwtService, Filter, ApplicationConfig |
| `backend/.../service/IAuthService.java` | New | register, login |
| `backend/.../service/impl/AuthServiceImpl.java` | New | Implementación |
| `frontend/src/contexts/AuthContext.jsx` | New | Context provider |
| `frontend/src/hooks/useAuth.js` | New | Custom hook |
| `frontend/src/pages/LoginPage.jsx` | New | Página login |
| `frontend/src/pages/RegisterPage.jsx` | New | Página registro |
| `frontend/src/components/Header/Header.jsx` | Modified | Avatar condicional |
| `frontend/src/services/api.js` | Modified | Token injection, 401 handling |
| `frontend/src/App.jsx` | Modified | AuthProvider + rutas |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| JWT secret hardcodeado en código | Medium | Dos capas: `@Value("${app.jwt.secret}")` en JwtService + fallback en `application.properties` + variable de entorno `APP_JWT_SECRET` (Spring Boot la resuelve automáticamente). En desarrollo usa el fallback, en producción/envío usa la env var. |
| SecurityConfig rompe endpoints de Sprint 1 | Medium | Mantener `/api/lodgings/**` público temporalmente. **Debe estar resuelto para fin de Sprint 2** — cuando se implementen #16 (admin) y el resto de rutas protegidas. |
| Token expirado sin refresh | High (aceptado) | Token JWT expira en 8 horas. Sin refresh token — el usuario vuelve a login. Aceptado para MVP. Documentado como: *"El token expira en 8 horas. Para una versión productiva, se agregaría refresh token rotation, pero está fuera del alcance del MVP."* |
| CORS entre frontend/backend | Low | Ya configurado en SecurityConfig |

## Rollback Plan

1. Revertir commit(s) del change en la rama `sprint-2`
2. Restaurar `SecurityConfig.java` a estado anterior (permit-all en `/api/**`)
3. Eliminar dependencias jjwt de `pom.xml`
4. Frontend: revertir `App.jsx`, `Header.jsx`, `api.js` a estado pre-auth
5. Las tablas nuevas (`users`, `roles`) pueden quedar — no afectan funcionalidad existente

## Dependencies

- MariaDB corriendo con schema actualizado (tabla `users`)
- jjwt 0.12.6 disponible en Maven Central
- `jwt-decode` npm package en frontend

## Success Criteria

- [ ] Usuario se registra con nombre, apellido, email, contraseña (validaciones incluidas)
- [ ] Usuario loguea con email+contraseña, recibe JWT
- [ ] Frontend muestra avatar con iniciales + nombre tras login
- [ ] "Cerrar sesión" limpia token y estado, vuelve a modo anónimo
- [ ] `/auth/**` accesible sin token, resto protegido
- [ ] Tests backend pasan (unitarios + integración)
- [ ] No se rompen endpoints de lodgings (Sprint 1)
