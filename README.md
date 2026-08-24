# TuHospedaje

Plataforma web para la búsqueda y reserva de alojamientos turísticos. Permite a los usuarios explorar hospedajes por ciudad y fechas, guardar favoritos, hacer reservas, y recibir confirmación por email. Los administradores gestionan alojamientos, categorías, características, políticas e imágenes vía Cloudinary.

Proyecto final integrador — Digital House.

---

## Tecnologías

### Backend
- Java 17
- Spring Boot 3.5
- Spring Security + JWT (jjwt 0.12.6)
- Spring Data JPA / MariaDB
- Cloudinary
- Spring Mail (Mailtrap)
- Testcontainers (tests de integración)

### Frontend
- React 19 + Vite 8
- React Router 7
- Lucide React
- jwt-decode

### E2E
- Playwright (Chromium + Firefox + mobile Chromium)

---

## Instalación local

### Requisitos
- Java 17+
- Node.js 18+
- MariaDB
- Maven (o usar el wrapper incluido)

### Clonar el repositorio
```bash
git clone https://github.com/GinoL221/tuhospedaje.git
cd tuhospedaje
```

---

### Backend (`/backend`)

#### Crear la base de datos
```sql
CREATE DATABASE tuhospedaje;
```

#### Configurar variables de entorno
```bash
cp backend/.env.example backend/.env
```

**Archivo `.env` (backend):**
```dotenv
# Base de datos
DB_USERNAME=tuhospedaje
DB_PASSWORD=your_password

# JWT — must be Base64-encoded and decode to at least 256 bits (32 bytes).
# A plain passphrase will NOT work. Generate one with: openssl rand -base64 48
JWT_SECRET=change-me-generate-with-openssl-rand-base64-48

# CORS — URL del frontend
CORS_ALLOWED_ORIGINS=http://localhost:5173

# Cloudinary (requerido para upload de imágenes)
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret

# Email (opcional — false deshabilita SMTP y logea en consola)
MAIL_SMTP_ENABLED=false
MAILTRAP_HOST=sandbox.smtp.mailtrap.io
MAILTRAP_PORT=2525
MAILTRAP_USERNAME=your_username
MAILTRAP_PASSWORD=your_password

# Canonical lodging masters for the local dev profile (optional).
# Default: ~/TuHospedajeAssets/canonical-lodging-images
# Override when the external asset directory is mounted elsewhere.
TUHOSPEDAJE_CANONICAL_ASSETS_ROOT=/home/your-user/TuHospedajeAssets/canonical-lodging-images
```

> Para desarrollo local existe un perfil `dev` con defaults seguros (sin secretos reales).
> Activar con: `SPRING_PROFILES_ACTIVE=dev`
>
> Con el perfil `dev`, el backend sirve los masters JPEG externos en
> `http://localhost:8080/canonical-lodging-images/**`. El seed demo usa esas URLs
> locales y conserva los binarios fuera del repositorio. El root por defecto es
> `~/TuHospedajeAssets/canonical-lodging-images`; podés cambiarlo con
> `TUHOSPEDAJE_CANONICAL_ASSETS_ROOT`.

#### Correr el backend
```bash
cd backend
./mvnw spring-boot:run
```
> Disponible en `http://localhost:8080`

#### Ciclo de vida del esquema

Flyway aplica `db/migration/V1__baseline_schema.sql` en una base vacía y Hibernate valida el mapeo sin crear ni cambiar tablas. Los perfiles default y `prod` no cargan usuarios ni datos demo.

1. Para un entorno nuevo o descartable, creá la base y ejecutá el backend: Flyway aplica V1 automáticamente.
2. Para un cambio de esquema posterior, agregá una migración versionada nueva; nunca edites una migración ya aplicada.
3. Para revertir un despliegue en una base descartable, revertí la configuración de la aplicación y recreá la base desde el proceso anterior.

En producción, `DB_USERNAME`/`DB_PASSWORD` pertenecen a la cuenta de ejecución, sin permisos DDL. Flyway se ejecuta en cada inicio, por eso `DB_MIGRATION_USERNAME`/`DB_MIGRATION_PASSWORD` deben estar disponibles en cada inicio y reinicio para una cuenta distinta, limitada al DDL/DML necesario para migrar. Rotá esas credenciales periódicamente; no revoques ni deshabilites la cuenta después de una migración. El perfil `prod` falla al iniciar si faltan y no usa las credenciales de la aplicación como reemplazo.

#### Adopción controlada de una base existente

`baseline-on-migrate=false` sigue siendo el valor seguro. Una base no vacía creada por Hibernate requiere una adopción manual y planificada:

1. Detené todas las instancias de la aplicación y bloqueá escrituras. Tomá un backup restaurable y verificá la restauración en un entorno aislado.
2. Compará tablas, columnas, índices, claves y restricciones contra `V1__baseline_schema.sql`. Continuá únicamente si el esquema coincide exactamente y no hay una tabla `flyway_schema_history` parcial.
3. Con una versión compatible de Flyway y credenciales administrativas temporales, creá una configuración efímera que no exponga la contraseña en los argumentos del proceso y ejecutá una sola vez:
   ```bash
   FLYWAY_CONF=$(mktemp)
   chmod 600 "$FLYWAY_CONF"
   trap 'rm -f "$FLYWAY_CONF"' EXIT
   printf 'flyway.url=%s\nflyway.user=%s\nflyway.password=%s\nflyway.baselineVersion=1\nflyway.baselineDescription=existing schema adoption\n' \
     "$DB_URL" "$DB_ADMIN_USER" "$DB_ADMIN_PASSWORD" >"$FLYWAY_CONF"
   flyway -configFiles="$FLYWAY_CONF" baseline
   rm -f "$FLYWAY_CONF"
   trap - EXIT
   ```
4. Verificá que `flyway_schema_history` contenga un baseline exitoso en versión 1. Iniciá una instancia con las credenciales normales y confirmá que `migrate` no intenta ejecutar V1 y que Hibernate valida el esquema.
5. Quitá las credenciales administrativas temporales y recién entonces reabrí escrituras.

La frontera de rollback termina antes de ejecutar `baseline`: ante cualquier diferencia o error, no modifiques el esquema, restaurá el backup en una base nueva y volvé a la versión anterior de la aplicación. Después de registrar el baseline no borres ni edites manualmente `flyway_schema_history`; restaurá el backup completo para deshacer la adopción.

MariaDB puede confirmar cada sentencia DDL aunque una migración completa falle. En una base nueva no descartable, ante un fallo de V1: detené todas las instancias y bloqueá escrituras; guardá los logs; inspeccioná `flyway_schema_history` y compará el esquema real con V1 sin ejecutar `repair` ni reintentar. Restaurá el backup verificado en una base nueva, o eliminá y recreá la base únicamente si confirmaste que no contiene datos que deban conservarse. Corregí la causa fuera de producción y ejecutá V1 desde cero sobre esa base restaurada o recreada. No habilites `baseline-on-migrate`: puede adoptar por accidente el esquema equivocado.

---

### Frontend (`/frontend`)

#### Configurar variables de entorno
```bash
cp frontend/.env.example frontend/.env
```

**Archivo `.env` (frontend):**
```dotenv
VITE_API_URL=http://localhost:8080/api

# Opcional — si no se define, el botón flotante de WhatsApp no se muestra.
VITE_WHATSAPP_NUMBER=5491112345678
```

#### Correr el frontend
```bash
cd frontend
npm install
npm run dev
```
> Disponible en `http://localhost:5173`

---

## Endpoints (API REST)

> Swagger UI disponible en: `http://localhost:8080/swagger-ui/index.html`

### Autenticación

| Método | Endpoint              | Descripción               | Auth       |
|--------|-----------------------|---------------------------|------------|
| POST   | /api/auth/register    | Registro — JWT entregado en cookie `ACCESS_TOKEN` HttpOnly | ❌ Público |
| POST   | /api/auth/login       | Login — JWT entregado en cookie `ACCESS_TOKEN` HttpOnly (no expuesto en el body) | ❌ Público |
| POST   | /api/auth/logout      | Limpia la cookie de sesión (requiere CSRF válido) | ❌ Público |
| GET    | /api/auth/me          | Identidad de la sesión autenticada | ✅ Autenticado |
| GET    | /api/auth/csrf        | Bootstrap explícito del token CSRF | ✅ Autenticado |
| POST   | /api/auth/refresh     | Rota `REFRESH_TOKEN` por un nuevo `ACCESS_TOKEN` (CSRF-exempt: cookie httpOnly) | ❌ Público |
| POST   | /api/auth/password    | Cambia la contraseña y revoca todas las sesiones de refresh propias | ✅ Autenticado |

Las mutaciones usan protección CSRF vía cookie `XSRF-TOKEN` + header `X-XSRF-TOKEN`. Las sesiones de refresh (persistencia, rotación y detección de replay) están **activas por defecto** (`app.session.refresh.enabled=true`) e integradas al flujo HTTP completo: login/registro emiten una cookie `REFRESH_TOKEN` httpOnly además del `ACCESS_TOKEN`, `POST /api/auth/refresh` la intercambia por un nuevo `ACCESS_TOKEN` y rota el refresh token, y logout/cambio de contraseña revocan la sesión (o familia de sesiones) correspondiente.

### Alojamientos

| Método | Endpoint                          | Descripción                                       | Auth        |
|--------|-----------------------------------|---------------------------------------------------|-------------|
| GET    | /api/lodgings                     | Listar (soporta `page`, `size`, `category`)       | ❌ Público  |
| GET    | /api/lodgings/random              | Lista aleatoria                                   | ❌ Público  |
| GET    | /api/lodgings/{id}                | Detalle de alojamiento                            | ❌ Público  |
| GET    | /api/lodgings/search              | Buscar por ciudad, fechas, huéspedes, precio      | ❌ Público  |
| GET    | /api/lodgings/cities              | Ciudades disponibles (con filtro `?q=`)           | ❌ Público  |
| GET    | /api/lodgings/{id}/availability   | Disponibilidad para un rango de fechas            | ❌ Público  |
| POST   | /api/lodgings                     | Crear alojamiento                                 | ✅ ADMIN    |
| PUT    | /api/lodgings/{id}                | Actualizar alojamiento                            | ✅ ADMIN    |
| DELETE | /api/lodgings/{id}                | Eliminar alojamiento                              | ✅ ADMIN    |

### Categorías

| Método | Endpoint               | Descripción           | Auth       |
|--------|------------------------|-----------------------|------------|
| GET    | /api/categories        | Listar categorías     | ❌ Público |
| GET    | /api/categories/{id}   | Detalle de categoría  | ❌ Público |
| POST   | /api/categories        | Crear categoría       | ✅ ADMIN   |
| PUT    | /api/categories/{id}   | Actualizar categoría  | ✅ ADMIN   |
| DELETE | /api/categories/{id}   | Eliminar categoría    | ✅ ADMIN   |

### Características (Features)

| Método | Endpoint             | Descripción              | Auth       |
|--------|----------------------|--------------------------|------------|
| GET    | /api/features        | Listar características   | ❌ Público |
| GET    | /api/features/{id}   | Detalle                  | ❌ Público |
| POST   | /api/features        | Crear característica     | ✅ ADMIN   |
| PUT    | /api/features/{id}   | Actualizar               | ✅ ADMIN   |
| DELETE | /api/features/{id}   | Eliminar                 | ✅ ADMIN   |

### Políticas

| Método | Endpoint             | Descripción      | Auth       |
|--------|----------------------|------------------|------------|
| GET    | /api/policies        | Listar políticas | ❌ Público |
| GET    | /api/policies/{id}   | Detalle          | ❌ Público |
| POST   | /api/policies        | Crear política   | ✅ ADMIN   |
| PUT    | /api/policies/{id}   | Actualizar       | ✅ ADMIN   |
| DELETE | /api/policies/{id}   | Eliminar         | ✅ ADMIN   |

### Favoritos

| Método | Endpoint                      | Descripción                        | Auth           |
|--------|-------------------------------|------------------------------------|----------------|
| GET    | /api/favorites                | Mis favoritos                      | ✅ Autenticado |
| POST   | /api/favorites/{lodgingId}    | Agregar a favoritos                | ✅ Autenticado |
| DELETE | /api/favorites/{lodgingId}    | Quitar de favoritos                | ✅ Autenticado |

### Reservas

| Método | Endpoint                  | Descripción                        | Auth           |
|--------|---------------------------|------------------------------------|----------------|
| POST   | /api/reservations         | Crear reserva                      | ✅ Autenticado |
| GET    | /api/reservations/{id}    | Detalle de reserva                 | ✅ Autenticado |
| GET    | /api/reservations/my      | Mis reservas                       | ✅ Autenticado |
| GET    | /api/reservations         | Todas las reservas                 | ✅ ADMIN       |
| PATCH  | /api/reservations/{id}/cancel | Cancelar reserva propia (antes del check-in) | ✅ Autenticado |

### Calificaciones

| Método | Endpoint                          | Descripción              | Auth           |
|--------|-----------------------------------|--------------------------|----------------|
| POST   | /api/ratings                      | Calificar alojamiento    | ✅ Autenticado |
| GET    | /api/ratings/lodging/{lodgingId}  | Calificaciones del alojamiento | ❌ Público |

### Usuarios

| Método | Endpoint               | Descripción                   | Auth     |
|--------|------------------------|-------------------------------|----------|
| GET    | /api/users             | Listar usuarios               | ✅ ADMIN |
| PUT    | /api/users/{id}/role   | Cambiar rol de usuario        | ✅ ADMIN |

### Imágenes

| Método | Endpoint      | Descripción                      | Auth     |
|--------|---------------|----------------------------------|----------|
| POST   | /api/upload   | Subir imagen a Cloudinary        | ✅ ADMIN |

---

## Testing

### Backend (JUnit + Testcontainers)
```bash
cd backend
./mvnw test
```

### E2E — Playwright (requiere backend y frontend corriendo)
```bash
cd e2e
npm install
npx playwright test
```

Reportes generados en `e2e/playwright-report/`.

El workflow de CI ejecuta los proyectos `chromium`, `firefox` y `mobile-chromium`. En Sprint 4, `mobile-chromium` cubre el shell responsive y el flujo de reservas en viewport móvil.

---

## Documentación

- `docs/diseno/` — manual de identidad visual y paleta de colores
- `docs/markdown/project-definition.md` — definición del proyecto (alcance, roadmap, ADRs)
- `docs/markdown/sprint-{1..4}/` — reporte y test plan de cada sprint
- `docs/markdown/sprint-2/` — incluye el modelo de datos (`.mmd` / `.svg`)
- `docs/entregables/` — PDFs de la definición del proyecto, reports y test plans

---

## Sprints

| Sprint   | Estado          | Descripción                                                                    |
|----------|-----------------|--------------------------------------------------------------------------------|
| Sprint 1 | ✅ Completado   | Base del sistema, catálogo de alojamientos, panel de administración            |
| Sprint 2 | ✅ Completado   | Autenticación JWT, roles, categorías, Cloudinary                               |
| Sprint 3 | ✅ Completado   | Búsqueda, favoritos, galería con modal viewer, CRUD policies, íconos Lucide    |
| Sprint 4 | ✅ Completado   | Motor de reservas, historial, WhatsApp, email de confirmación, autenticación segura, cancelación de reservas, suite E2E y cobertura responsive/móvil de reservas |

## Ramas

- `main` — integración final
- `sprint-1` — base del sistema (congelada)
- `sprint-2` — auth + categorías (congelada)
- `sprint-3` — búsqueda + favoritos (congelada)
- `sprint-4` — reservas, autenticación segura, cancelación de reservas y E2E (congelada; integrada a `main` mediante el merge commit `8a3fd43`, PR #36)

---

## Autor

- [@GinoL221](https://github.com/GinoL221)

---

## Licencia

Uso educativo — Digital House.
