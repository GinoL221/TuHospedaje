# Frontend de TuHospedaje

Aplicación web React para explorar alojamientos, consultar disponibilidad, gestionar favoritos y reservas, y administrar el catálogo según el rol de la persona autenticada.

## Requisitos

- Node.js `^20.19.0 || >=22.12.0`, según Vite 8.2.2 fijado en `package-lock.json`.
- npm compatible con el lockfile v3.

El frontend está escrito en JavaScript y JSX. No usa TypeScript ni React Compiler.

## Instalación y desarrollo

Desde este directorio:

```bash
cd frontend
npm install
npm run dev
```

Para una instalación limpia y reproducible, también podés usar `npm ci`. El comando `dev` inicia Vite; la configuración no define un proxy propio ni un puerto alternativo.

### Variables de entorno públicas

Vite incorpora al bundle del navegador las variables con prefijo `VITE_`. No pongas secretos, tokens ni credenciales en ellas.

| Variable | Uso |
| --- | --- |
| `VITE_API_URL` | Base de las solicitudes HTTP del cliente en `src/services/api.js`. El código la consume directamente y no define un fallback. |
| `VITE_WHATSAPP_NUMBER` | Número que habilita el botón de WhatsApp en `src/components/WhatsAppButton/WhatsAppButton.jsx` cuando cumple el formato esperado. |

Definí los nombres necesarios en tu configuración local sin publicar sus valores. No se documentan valores concretos en este archivo.

## Scripts disponibles

Ejecutá estos comandos desde `frontend/`:

| Comando | Uso |
| --- | --- |
| `npm run dev` | Inicia el servidor de desarrollo de Vite. |
| `npm run build` | Genera el bundle de producción. |
| `npm run preview` | Sirve localmente el bundle generado para inspección. |
| `npm run lint` | Ejecuta ESLint sobre el paquete frontend. |
| `npm run test` | Ejecuta Vitest una vez. |
| `npm run test:watch` | Ejecuta Vitest en modo interactivo/watch. |
| `npm run coverage` | Ejecuta Vitest con cobertura V8. |
| `npm run format` | Formatea archivos fuente con Prettier y modifica archivos. |
| `npm run format:check` | Comprueba el formato con Prettier sin modificar archivos. |

El hook versionado de pre-commit conserva Gitleaks y aplica el formateo staged mediante `frontend/scripts/pre-commit.sh`.

## Arquitectura

- `src/main.jsx`: punto de entrada; monta React, Inter y los estilos globales.
- `src/App.jsx`: `BrowserRouter`, shell público, carga diferida de páginas, guards de autenticación, fallback de carga y boundary de errores de chunks.
- `src/components/`: componentes reutilizables, navegación, formularios, diálogos y piezas del panel.
- `src/pages/`: pantallas agrupadas por recorrido, como Home, detalle, reservas, favoritos y administración.
- `src/context/AuthContext.jsx`: estado global de autenticación.
- `src/hooks/`: hooks de autenticación, disponibilidad, búsqueda, tablas, recomendaciones y reservas.
- `src/services/`: acceso HTTP y servicios de dominio.
- `src/utils/`: utilidades compartidas.

### Rutas principales

Las rutas se declaran en [`src/App.jsx`](src/App.jsx):

- Públicas: `/`, `/search` (redirige al inicio conservando la búsqueda), `/login`, `/register`, `/lodgings/:id`, `/unauthorized` y la ruta de no encontrado.
- Requieren autenticación: `/booking/:lodgingId`, `/booking/confirmation`, `/my-reservations` y `/favorites`.
- Requiere rol ADMIN: `/administración`. `/admin` es un alias que redirige a la ruta canónica.

Pantallas principales:

- [`src/pages/Home/Home.jsx`](src/pages/Home/Home.jsx)
- [`src/pages/ProductDetail/ProductDetail.jsx`](src/pages/ProductDetail/ProductDetail.jsx)
- [`src/pages/Booking/`](src/pages/Booking/)
- [`src/pages/Favorites/FavoritesPage.jsx`](src/pages/Favorites/FavoritesPage.jsx)
- [`src/pages/MyReservations/MyReservationsPage.jsx`](src/pages/MyReservations/MyReservationsPage.jsx)
- [`src/pages/Admin/Admin.jsx`](src/pages/Admin/Admin.jsx)

### Servicios y solicitudes

- [`src/services/api.js`](src/services/api.js): cliente HTTP compartido con `credentials: "include"`, CSRF para mutaciones, deadline de 15 segundos y un reintento después de refrescar la sesión.
- [`src/services/refreshCoordinator.js`](src/services/refreshCoordinator.js): coordina el refresh de sesión cuando varias solicitudes reciben `401`.
- [`src/services/authService.js`](src/services/authService.js): login, registro y sesión.
- [`src/services/lodgingService.js`](src/services/lodgingService.js): alojamientos, búsqueda, ciudades, disponibilidad y recomendaciones.
- [`src/services/reservationService.js`](src/services/reservationService.js): reservas y cancelaciones.
- [`src/services/favoriteService.js`](src/services/favoriteService.js): favoritos.
- [`src/services/ratingService.js`](src/services/ratingService.js): calificaciones.
- [`src/services/uploadService.js`](src/services/uploadService.js): uploads multipart.
- [`src/services/adminCatalogService.js`](src/services/adminCatalogService.js): operaciones del catálogo administrativo.
- [`src/services/categoryService.js`](src/services/categoryService.js): categorías públicas.

Los guards [`src/components/RequireAuth.jsx`](src/components/RequireAuth.jsx) y [`src/components/RequireAdmin.jsx`](src/components/RequireAdmin.jsx) protegen los recorridos que requieren sesión o rol.

## Testing y cobertura

Vitest está configurado en [`vite.config.js`](vite.config.js) con entorno `jsdom`, globals, CSS deshabilitado para los tests y setup en [`src/test/setup.js`](src/test/setup.js). Los tests se mantienen junto al código, usando nombres `*.test.js` y `*.test.jsx`.

`npm run coverage` genera reportes de texto y HTML en `frontend/coverage/`. Los umbrales configurados son:

- Statements: `85%`
- Branches: `80%`
- Functions: `74%`
- Lines: `87%`

Los escenarios Playwright no viven en este paquete: la configuración y los tests E2E están en el directorio hermano [`../e2e/`](../e2e/). El frontend README no afirma que esos escenarios puedan ejecutarse sin el backend, la base de datos y los servicios requeridos.

## Alcance de este README

Este archivo documenta el paquete frontend y sus comandos observables. No declara preparación para producción, entrega externa de email o WhatsApp, disponibilidad de infraestructura, resultados de CI para revisiones futuras ni soporte de funcionalidades que el código no implementa.
