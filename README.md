# TuHospedaje

Plataforma web de reservas de alojamientos turísticos.
Proyecto final integrador — Digital House.

## Stack

- **Backend:** Java 21 / Spring Boot 3.5 / Spring Data JPA / MariaDB / Testcontainers
- **Frontend:** React 19 / Vite / React Router / Lucide React
- **Herramientas:** Maven, Lombok, Pandoc

## Sprints

| Sprint | Estado | Descripción |
|--------|--------|-------------|
| Sprint 1 | ✅ Completado | Base del sistema, catálogo de alojamientos, panel de administración |
| Sprint 2 | ✅ Completado | Autenticación JWT, roles, categorías, Cloudinary |
| Sprint 3 | ✅ Completado | Búsqueda, favoritos, galería con modal viewer, CRUD policies, íconos Lucide |
| Sprint 4 | 🔜 Pendiente | Motor de reservas, disponibilidad |

## Ramas

- `main` — integración final
- `sprint-1` — base del sistema (congelada)
- `sprint-2` — auth + categorías (congelada)
- `sprint-3` — trabajo actual

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

## Documentación

- `markdown/sprint-1/` — reporte, test plan y PDFs del Sprint 1
- `markdown/sprint-2/` — reporte, test plan, modelo de datos y PDFs del Sprint 2
- `markdown/sprint-3/` — reporte, test plan y PDFs del Sprint 3

## Licencia

Uso educativo — Digital House.
