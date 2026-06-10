# TuHospedaje

Plataforma web de reservas de alojamientos turísticos.
Proyecto final integrador — Digital House.

## Stack

- **Backend:** Java 21 / Spring Boot 3.5 / Spring Data JPA / MariaDB / Testcontainers
- **Frontend:** React 19 / Vite / React Router / Lucide React
- **E2E:** Playwright (Chromium + Firefox, regresión visual)
- **Herramientas:** Maven, Lombok, Pandoc

## Sprints

| Sprint | Estado | Descripción |
|--------|--------|-------------|
| Sprint 1 | ✅ Completado | Base del sistema, catálogo de alojamientos, panel de administración |
| Sprint 2 | ✅ Completado | Autenticación JWT, roles, categorías, Cloudinary |
| Sprint 3 | ✅ Completado | Búsqueda, favoritos, galería con modal viewer, CRUD policies, íconos Lucide |
| Sprint 4 | ✅ Completado | Motor de reservas, historial, WhatsApp, email de confirmación, suite E2E con Playwright |

## Ramas

- `main` — integración final
- `sprint-1` — base del sistema (congelada)
- `sprint-2` — auth + categorías (congelada)
- `sprint-3` — búsqueda + favoritos (congelada)
- `sprint-4` — reservas + E2E (congelada)

## Cómo correr

### Backend
```bash
cd backend
mvn spring-boot:run
```

### Frontend
```bash
cd frontend
npm install
npm run dev
```

### Tests E2E (requiere backend y frontend corriendo)
```bash
cd e2e
npm install
npx playwright test
```

## Documentación

- `docs/markdown/sprint-1/` … `docs/markdown/sprint-4/` — reporte y test plan de cada sprint
- `docs/markdown/sprint-2/` — incluye el modelo de datos (`.mmd` / `.svg`)
- `docs/entregables/` — PDFs de reports y test plans (todos los sprints)

## Licencia

Uso educativo — Digital House.
