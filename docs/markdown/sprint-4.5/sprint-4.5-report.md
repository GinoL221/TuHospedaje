---
title: "Bitácora de Ejecución y Cierre — Sprint 4.5"
subtitle: "TuHospedaje — Tablas Uniformes y Dashboard de Reservas"
author: "Equipo de Desarrollo"
date: "Junio 2026"
pdf_options:
  format: a4
  margin:
    top: 25mm
    bottom: 25mm
    left: 20mm
    right: 20mm
  displayHeaderFooter: true
  headerTemplate: |
    <div style="font-size: 9pt; width: 100%; text-align: right; padding-right: 20mm; color: #666;">
      TuHospedaje — Documentación Técnica Oficial
    </div>
  footerTemplate: |
    <div style="font-size: 9pt; width: 100%; display: flex; justify-content: space-between; padding: 0 20mm; color: #666;">
      <div>Sprint 4.5 — Junio 2026</div>
      <div>Página <span class="pageNumber"></span> de <span class="totalPages"></span></div>
    </div>
---

<style>
.page-break { page-break-before: always; }
table { width: 100%; } table, tr { page-break-inside: avoid; }
h1, h2, h3, h4 { page-break-after: avoid; }
</style>

# BITÁCORA DE EJECUCIÓN Y CIERRE — SPRINT 4.5

**Foco del Incremento:** Tablas de Administración Uniformes y Panel de Control (Dashboard) con Reservas Recientes
**Stack Tecnológico:** Java 17 / Spring Boot 3.5 / Spring Security 6 / MariaDB / React 19 / Vite / Testcontainers / SpringDoc OpenAPI

## 1. Resumen del Incremento (Scope)

El Sprint 4.5 consolidó la experiencia de usuario en el Panel de Administración y extendió las capacidades del Dashboard mediante dos ejes principales: la unificación de tablas y la visualización de reservas recientes.

En primer lugar, se estandarizó la lógica de presentación de datos en la interfaz mediante el desarrollo del hook personalizado `useTableData` y los componentes reutilizables `SortableTh` (para encabezados ordenables) y `Pagination` (para control de páginas). Estas herramientas se integraron en las vistas de administración de Categorías, Características, Políticas y Usuarios. Para mantener la consistencia del panel, se migró la tabla de alojamientos (`AdminLodgings`) de paginación del servidor a paginación del cliente (mediante una petición plana `GET /api/lodgings`), agregando además la columna de descripción y alineando la disposición y orden del catálogo administrativo. Adicionalmente, se integró el componente de paginación unificado en la sección de `SearchResults`.

En segundo lugar, se implementó el soporte del lado del servidor y cliente para visualizar las reservas recientes en el Dashboard del administrador. Se introdujo el endpoint `GET /api/reservations` (exclusivo para rol ADMIN) que retorna todas las reservas de la plataforma ordenadas de forma descendente por su identificador. La UI del Dashboard se enriqueció con una tarjeta de estadísticas de "Reservas" y una sección dedicada a "Últimas reservas" que renderiza las primeras 4 transacciones de la plataforma. Toda la maquetación se adaptó de forma responsiva mediante estilos dedicados en `Admin.css` que ordenan las secciones de forma paralela o apilada según la resolución.

Finalmente, el incremento se verificó exhaustivamente mediante la ejecución exitosa de pruebas de integración con Testcontainers en el backend, junto con compilaciones (build) y análisis de estática de código (lint) sin errores en el frontend.

## 2. Arquitectura del Sistema e Integración

### 2.1. Backend (Spring Boot + Spring Security 6)

Se expandió la arquitectura del backend para proporcionar una consulta de reservas totales para usuarios administradores. Los cambios se realizaron dentro de las capas habituales de persistencia, servicio y controladoras:

```
Controller → Service (Interface + Impl) → Repository → DTO
```

#### Matriz de Componentes Introducidos o Modificados:

| Módulo | Entidad / DTO Afectado | Cambio en Capa de Servicio | Cambio en Controller |
|--------|------------------------|---------------------------|----------------------|
| **Reservas** | `Reservation`, `ReservationResponse` | `ReservationService` / `ReservationServiceImpl`: obtener todas las reservas de la plataforma ordenadas por id desc | `ReservationController`: `GET /api/reservations` protegido para ADMIN |

* **Restricción de Acceso (RBAC):** El endpoint `/api/reservations` está restringido estrictamente a administradores mediante la anotación `@PreAuthorize("hasRole('ADMIN')")`. Las pruebas de integración en `ReservationControllerIntegrationTest` confirman que peticiones sin credenciales reciben HTTP 401, usuarios no-administradores reciben HTTP 403, y administradores acceden con HTTP 200 recibiendo el listado completo ordenado por ID desc.
* **Consultas del Repositorio:** Se declaró el método `findAllByOrderByIdDesc()` en `ReservationRepository` aprovechando la generación automática de queries basada en firmas de Spring Data JPA.

### 2.2. Frontend (React + Vite)

Evolución de la SPA con el desarrollo de un hook personalizado, dos componentes compartidos de presentación de tablas y actualizaciones en el panel administrativo:

```
src/
├── components/
│   ├── SortableTh/
│   │   └── SortableTh.jsx         (nuevo — encabezado de columna ordenable)
│   └── Pagination/
│       └── Pagination.jsx         (nuevo — control de paginación reutilizable)
├── hooks/
│   └── useTableData.js            (nuevo — hook de ordenamiento y paginación cliente)
├── pages/
│   └── Admin/
│       ├── AdminCategories.jsx    (modificado — usa useTableData + componentes reutilizables)
│       ├── AdminFeatures.jsx      (modificado — usa useTableData + componentes reutilizables)
│       ├── AdminPolicies.jsx      (modificado — usa useTableData + componentes reutilizables)
│       ├── AdminUsers.jsx         (modificado — usa useTableData + componentes reutilizables)
│       ├── AdminLodgings.jsx      (modificado — fetch plano de alojamientos y paginación en cliente)
│       ├── LodgingsTable.jsx      (modificado — removidos props de servidor, agregada columna Desc y componentes)
│       ├── AdminDashboard.jsx     (modificado — sección Últimas reservas, stats card de Reservas)
│       └── Admin.css              (modificado — estilos de tabla ordenable, indicadores y grid de dashboard)
└── SearchResults.jsx              (modificado — reemplazo de paginación por componente Pagination)
```

* **`useTableData` Hook:** Encapsula el estado de página actual, ordenamiento de columna (`sortKey`, `direction`) y búsqueda/filtrado. Provee datos procesados (`paginatedData`), funciones para alternar ordenamiento y manejar cambio de páginas.
* **Componentes `SortableTh` y `Pagination`:** Aislan la lógica visual. `SortableTh` añade indicadores visuales (`▲`/`▼`) de forma condicional al hacer click. `Pagination` unifica la barra de paginado con soporte de deshabilitación de botones y la clase `className` configurable para alinearse con diferentes contenedores (por ejemplo, en `SearchResults`).
* **Migración a Client-Side en Alojamientos:** Se simplificó `AdminLodgings` eliminando la lógica de páginas remotas. Ahora realiza un `GET /api/lodgings` plano para obtener el listado completo y delega el particionado y ordenamiento a `useTableData` local. Esto homogeneiza el comportamiento respecto a las demás entidades administrativas.

## 3. Trazabilidad de Historias de Usuario (User Stories)

| ID | Historia de Usuario | Componente / Vista UI | Endpoint Backend | Criterio de Aceptación / Estado |
|----|---------------------|----------------------|------------------|--------------------------------|
| **US #36** | Visualizar tablas administrativas uniformes con ordenación y paginación local. | `AdminCategories`, `AdminFeatures`, `AdminPolicies`, `AdminUsers`, `AdminLodgings` | N/A (Frontend) | Tablas con estilos homogéneos, indicadores visuales de ordenación y paginado consistente. |
| **US #37** | Acceder a estadísticas de reservas y a la sección de reservas recientes. | `AdminDashboard.jsx`, `Admin.css` | `GET /api/reservations` | Tarjeta de estadística activa. Tabla de "Últimas reservas" con las 4 transacciones más recientes ordenadas por id desc. Acceso restringido a ADMIN. |

## 4. Catálogo de Endpoints Nuevos / Modificados

### 4.1. Reservas (Administración)

| Método | Endpoint | Acceso (RBAC) | Descripción |
|--------|----------|---------------|-------------|
| GET | `/api/reservations` | ADMIN | Retorna todas las reservas del sistema, ordenadas por identificador de forma descendente (`id DESC`). |

<div style="page-break-before: always;"></div>

## 5. Modelo de Datos

### Queries y Lógica de Negocio en Backend

```
┌──────────────────────────────────────────────────┐
│              RESERVATIONS ENDPOINT               │
├──────────────────────────────────────────────────┤
│ GET /api/reservations (ADMIN)                    │
│ Retorna lista de ReservationResponse ordenada    │
│ de forma descendente por id.                     │
└──────────────────────────────────────────────────┘
```

* **findAllByOrderByIdDesc():** Consulta añadida al repositorio `ReservationRepository` que recupera la totalidad del histórico de reservas ordenado del ID mayor al menor, permitiendo que la interfaz consuma los registros más recientes de manera inmediata.

## 6. Decisiones Técnicas Clave

* **Ordenamiento y paginación en el cliente (Client-Side):** Se adoptó paginación local a través del hook personalizado `useTableData` para todas las entidades del panel administrativo, incluyendo `Lodging`. Al tratarse de tablas de configuración con volúmenes de datos bajos/medios, la carga única simplifica el backend, reduce llamadas redundantes a la base de datos y provee una experiencia inmediata al ordenar o pasar de página en el navegador.
* **Estandarización de Componentes de Presentación:** La separación de `SortableTh` y `Pagination` permite reutilizar la UI y estilos en distintas partes del sistema (como la integración del componente de paginación en la vista `SearchResults`), asegurando consistencia visual.
* **TDD y Seguridad robusta en backend:** El desarrollo de la sección de reservas del dashboard se abordó implementando primero la suite `ReservationControllerIntegrationTest`. Esto aseguró que la restricción de seguridad (`hasRole('ADMIN')`) estuviese plenamente operativa antes de implementar los servicios o consumir el endpoint en la UI.

## 7. Testing

* **Tests Backend Integrados:** Se crearon y ejecutaron con éxito pruebas específicas en `ReservationControllerIntegrationTest` (usando Testcontainers) para asegurar la seguridad basada en roles (ADMIN habilitado, USER bloqueado) y el ordenamiento inverso.
* **Análisis y Compilación Frontend:** Se ejecutó `npm run build` en el frontend compilando satisfactoriamente los 2122 módulos y `npm run lint` garantizando que no se introdujeran advertencias ni fallos de formato en el código estático.

## 8. Limitaciones Conocidas y Deuda Técnica Controlada

1. **Volumen de Datos en Paginación Cliente:** Si el catálogo de alojamientos, usuarios o reservas crece significativamente a miles de registros, la aproximación de paginación del lado del cliente podría degradar el tiempo de carga inicial. Se contempla migrar a paginación por base de datos (Spring Pageable) en el futuro únicamente si la volumetría de producción lo requiere.
2. **Acciones en Reservas Recientes:** La sección "Últimas reservas" del dashboard es puramente informativa. No se proveen acciones administrativas directas (como cancelar o reprogramar) sobre este listado en la pantalla actual.
