# Bitácora de Ejecución y Cierre — Sprint 2

**Foco del Incremento:** Seguridad y Organización — Autenticación JWT, Roles, Categorías y Características
**Stack Tecnológico:** Java 17 / Spring Boot 3.5 / Spring Security 6 / MariaDB / React 19 / Vite

---

## 1. Resumen del Incremento (Scope)

El Sprint 2 implementó la capa de autenticación y autorización del sistema, junto con la organización del catálogo mediante categorías y características. Se desarrolló registro de usuarios, login con JWT, cierre de sesión, panel de administración de usuarios para gestión de roles, CRUD completo de categorías y características, y visualización de características en el detalle del producto. También se incorporó notificación por email al registrarse.

---

## 2. Arquitectura del Sistema e Integración

### 2.1. Backend (Spring Boot)

Se mantuvo la arquitectura en capas del Sprint 1, extendiéndola con los nuevos módulos:

```
Controller → Service (Interface + Impl) → Repository → Entity / DTO
```

**Módulos nuevos:**

| Módulo | Entity | DTO | Service | Controller |
|--------|--------|-----|---------|------------|
| Auth | User | RegisterRequest, LoginRequest, AuthResponse | AuthService | AuthController |
| Categories | Category | CategoryDTO | CategoryService | CategoryController |
| Features | Feature | FeatureDTO | FeatureService | FeatureController |
| Users | — | UserDTO, RoleRequest | UserService | UserController |
| Email | — | — | EmailService | — |

**Decisiones de arquitectura:**

- **Servicios sin prefijo I:** Los servicios se renombraron de `IAuthService` → `AuthService`, `ILodgingService` → `LodgingService`, `ICategoryService` → `CategoryService` para mantener consistencia.
- **JWT:** HS256 con 8h de expiración, sin refresh token (aceptado para MVP). Secret configurable por `app.jwt.secret` + variable de entorno `APP_JWT_SECRET`.
- **Method Security:** Se habilitó `@EnableMethodSecurity` con `@PreAuthorize("hasRole('ADMIN')")` en endpoints de escritura de categorías, características y usuarios.
- **Email:** Servicio de email por consola configurable; en Sprint 2 se integró con Mailtrap SMTP (sandbox) para captura de correos en desarrollo.

### 2.2. Frontend (React + Vite)

Se extendió la estructura del frontend con nuevas páginas y componentes:

```
src/
├── components/
│   ├── Header/Header.jsx         (+ avatar admin redirect)
│   └── ConfirmDialog.jsx         (nuevo — confirmación reutilizable)
├── hooks/
│   ├── useAuth.js                (existente)
│   └── useConfirmCancel.js       (nuevo — cancelar con confirmación)
├── pages/
│   ├── Home/Home.jsx             (+ categorías funcionales + filtro)
│   ├── LoginPage.jsx             (nuevo — login con validación)
│   ├── RegisterPage.jsx          (nuevo — registro con feedback)
│   ├── ProductDetail/            (+ bloque de características)
│   └── Admin/
│       ├── Admin.jsx             (+ tabs: Usuarios, Características)
│       ├── AdminLodgings.jsx     (extraído)
│       ├── AdminCategories.jsx   (extraído)
│       ├── AdminFeatures.jsx     (nuevo)
│       └── AdminUsers.jsx        (nuevo)
├── context/AuthContext.jsx       (nuevo — lazy init, JWT)
└── services/api.js               (+ JWT injection, 204 handling)
```

---

## 3. Trazabilidad de Historias de Usuario (User Stories)

| ID | Historia de Usuario | Componente / Vista UI | Endpoint Asociado | Criterio de Aceptación / Estado |
|----|---------------------|----------------------|-------------------|--------------------------------|
| #12 | Categorizar productos | AdminCategories.jsx | POST/GET/PUT/DELETE /api/categories | CRUD completo con validación de nombre único y borrado bloqueado si hay alojamientos vinculados |
| #13 | Registrar usuario | RegisterPage.jsx | POST /api/auth/register | Formulario con nombre, apellido, email, contraseña + validaciones + feedback en tiempo real |
| #14 | Identificar usuario | LoginPage.jsx | POST /api/auth/login | Login con email+contraseña, JWT, errores claros, avatar con iniciales |
| #15 | Cerrar sesión | Header.jsx | N/A (lado cliente) | Botón "Cerrar sesión" bajo el avatar, limpieza de token, navegación anónima |
| #16 | Identificar administrador | AdminUsers.jsx | GET /api/users, PUT /api/users/{id}/role | Listado de usuarios, botón para asignar/quitar admin, protegido contra auto-cambio |
| #17 | Administrar características | AdminFeatures.jsx | POST/GET/PUT/DELETE /api/features | CRUD completo con nombre e ícono |
| #18 | Visualizar características | ProductDetail.jsx | Contenido de lodging.features | Bloque "Qué ofrece este lugar?" con íconos y nombres en grilla responsiva |
| #19 | Email confirmación registro | ConsoleEmailService → Mailtrap | N/A (trigger post-registro) | Email de bienvenida con nombre, email y link de login |
| #20 | Crear sección de categorías | Home.jsx | GET /api/categories, GET /api/lodgings?category= | Tags clickeables en Home que filtran alojamientos por categoría |
| #21 | Agregar categoría | AdminCategories.jsx | POST /api/categories | Modal con nombre y descripción, visible en panel admin |

---

## 4. Catálogo de Endpoints de la API REST

### Sprint 1 (heredados)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/lodgings` | Crear alojamiento |
| GET | `/api/lodgings` | Listar (con paginación) |
| GET | `/api/lodgings/random` | Recomendaciones aleatorias |
| GET | `/api/lodgings/search` | Búsqueda por nombre |
| GET | `/api/lodgings/{id}` | Detalle |
| PUT | `/api/lodgings/{id}` | Actualizar |
| DELETE | `/api/lodgings/{id}` | Eliminar |

### Sprint 2 (nuevos)

| Método | Endpoint | Auth | Descripción |
|--------|----------|------|-------------|
| POST | `/api/auth/register` | Público | Registrar usuario |
| POST | `/api/auth/login` | Público | Iniciar sesión |
| POST | `/api/categories` | ADMIN | Crear categoría |
| GET | `/api/categories` | Público | Listar categorías |
| GET | `/api/categories/{id}` | Público | Categoría por ID |
| PUT | `/api/categories/{id}` | ADMIN | Actualizar categoría |
| DELETE | `/api/categories/{id}` | ADMIN | Eliminar categoría |
| POST | `/api/features` | ADMIN | Crear característica |
| GET | `/api/features` | Público | Listar características |
| GET | `/api/features/{id}` | Público | Característica por ID |
| PUT | `/api/features/{id}` | ADMIN | Actualizar característica |
| DELETE | `/api/features/{id}` | ADMIN | Eliminar característica |
| GET | `/api/users` | ADMIN | Listar usuarios |
| PUT | `/api/users/{id}/role` | ADMIN | Cambiar rol de usuario |

---

## 5. Modelo de Datos y Cardinalidad

```
  ┌───────────────┐                  ┌────────────────────┐
  │    LODGING    │ 1              N │   LODGING_IMAGE    │
  ├───────────────┤──────────────────┤────────────────────┤
  │ id (PK)       │                  │ id (PK)            │
  │ name          │                  │ url                │
  │ description   │                  │ lodging_id (FK)    │
  │ ...           │                  └────────────────────┘
  │ category_id   │ N ──── 1 ┌──────────────┐
  └───────────────┘          │  CATEGORY    │
       M                    ├──────────────┤
       │                    │ id (PK)      │
       │                    │ name         │
  ┌────┴────────┐           │ description  │
  │  FEATURE    │           └──────────────┘
  ├─────────────┤
  │ id (PK)     │           ┌────────────────────┐
  │ name        │           │       USER         │
  │ icon        │           ├────────────────────┤
  └─────────────┘           │ id (PK)            │
                            │ first_name         │
  ┌──────────────────┐      │ last_name          │
  │ LODGING_FEATURE  │      │ email (unique)     │
  ├──────────────────┤      │ password           │
  │ lodging_id (FK)  │      │ role (USER/ADMIN)  │
  │ feature_id (FK)  │      │ image_url          │
  └──────────────────┘      └────────────────────┘
```

Relaciones:
- `Lodging N` → `1 Category` (nullable, FK category_id)
- `Lodging M` ↔ `N Feature` (tabla intermedia lodging_features)
- `Lodging 1` → `N LodgingImage` (cascade ALL)

---

## 6. Decisiones Técnicas Clave

- **JWT sin refresh token:** Aceptado para MVP. Token HS256 con 8h de expiración.
- **Separación AuthService / UserService:** `AuthService` conoce AuthenticationManager y JwtService. `UserService` solo para CRUD de usuarios y roles. Testing más limpio.
- **Servicios sin prefijo I:** Se renombraron todos los servicios eliminando la `I` de interfaz (`IAuthService` → `AuthService`), por consistencia con el nombre del archivo y preferencia del desarrollador.
- **@Transactional en tests de integración:** Todos los tests @SpringBootTest llevan @Transactional para evitar que deleteAll() en @BeforeEach borre datos reales de la BD.
- **useConfirmCancel hook:** Lógica de confirmación al cancelar formularios admin extraída a hook reutilizable.
- **Validación manual en formularios:** noValidate + validate() + fieldErrors en todos los formularios admin, consistente con login/register.
- **Bloqueo de borrado de categorías:** Si hay alojamientos usando una categoría, el DELETE devuelve error 400 con mensaje claro.

---

## 7. Limitaciones Conocidas y Deuda Técnica

- **Imágenes:** Sin carga real de archivos. Se usan URLs externas de picsum.photos.
- **Buscador del Home:** Sigue siendo placeholder visual (Sprint 3).
- **Favoritos:** No implementado (Sprint 3).
- **Puntuaciones:** No implementado (Sprint 3).
- **Reservas:** No implementado (Sprint 4).
