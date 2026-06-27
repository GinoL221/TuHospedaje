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
- Playwright (Chromium + Firefox)

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
```

> Para desarrollo local existe un perfil `dev` con defaults seguros (sin secretos reales).
> Activar con: `SPRING_PROFILES_ACTIVE=dev`

#### Correr el backend
```bash
cd backend
./mvnw spring-boot:run
```
> Disponible en `http://localhost:8080`

---

### Frontend (`/frontend`)

#### Configurar variables de entorno
```bash
cp frontend/.env.example frontend/.env
```

**Archivo `.env` (frontend):**
```dotenv
VITE_API_URL=http://localhost:8080/api
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
| POST   | /api/auth/register    | Registro de usuario       | ❌ Público |
| POST   | /api/auth/login       | Login — devuelve JWT      | ❌ Público |

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

---

## Documentación

- `docs/diseno/` — manual de identidad visual y paleta de colores
- `docs/markdown/project-definition.md` — definición del proyecto (alcance, roadmap, ADRs)
- `docs/markdown/sprint-{1..4}/` — reporte y test plan de cada sprint
- `docs/markdown/sprint-2/` — incluye el modelo de datos (`.mmd` / `.svg`)
- `docs/entregables/` — PDFs de la definición del proyecto, reports y test plans
- `TuHospedaje.postman_collection.json` — colección Postman lista para importar

---

## Sprints

| Sprint   | Estado          | Descripción                                                                    |
|----------|-----------------|--------------------------------------------------------------------------------|
| Sprint 1 | ✅ Completado   | Base del sistema, catálogo de alojamientos, panel de administración            |
| Sprint 2 | ✅ Completado   | Autenticación JWT, roles, categorías, Cloudinary                               |
| Sprint 3 | ✅ Completado   | Búsqueda, favoritos, galería con modal viewer, CRUD policies, íconos Lucide    |
| Sprint 4 | ✅ Completado   | Motor de reservas, historial, WhatsApp, email de confirmación, suite E2E       |

## Ramas

- `main` — integración final
- `sprint-1` — base del sistema (congelada)
- `sprint-2` — auth + categorías (congelada)
- `sprint-3` — búsqueda + favoritos (congelada)
- `sprint-4` — reservas + E2E (congelada)

---

## Autor

- [@GinoL221](https://github.com/GinoL221)

---

## Licencia

Uso educativo — Digital House.
