---
title: "Propuesta — PR 1: Búsqueda y Disponibilidad"
change: pr1-search-availability
sprint: 3
status: proposal
created: 2026-05-29
author: "Equipo de Desarrollo"
---

# Propuesta: Búsqueda (#22) y Disponibilidad (#23)

## Alcance

### Incluye

| ID | Historia | Descripción |
|----|----------|-------------|
| #22 | Realizar búsqueda | Bloque de búsqueda en Home con autocompletado de ciudades, selección de fechas (check-in / check-out), y página de resultados con filtros laterales. |
| #23 | Visualizar disponibilidad | Calendario doble en el detalle del producto mostrando fechas disponibles y ocupadas. |

### Queda fuera de este PR

- **Favoritos (#24, #25)** → PR 2
- **Políticas, compartir, reseñas, eliminar categoría (#26-#29)** → PR 3
- **Subida de imágenes reales** (picsum.photos sigue como placeholder)
- **Refresh tokens** (sesión simple de 8h se mantiene)
- **Precios por temporada** (precio fijo por noche)

## Enfoque técnico

### Backend

1. **Modelo de datos**
   - Agregar `pricePerNight` (BigDecimal) y `maxGuests` (Integer) a `Lodging`
   - Crear entidad `Reservation`: lodging, checkIn, checkOut, guestName, guestEmail, totalPrice, status
   - `@Version` en Reservation para optimistic locking contra reservas concurrentes

2. **Endpoints**
   - `GET /api/lodgings/search?city=&checkIn=&checkOut=&guests=&category=&minPrice=&maxPrice=`
     - Query JPQL que cruza disponibilidad: excluye lodgings con reservas solapadas en el rango de fechas
   - `GET /api/lodgings/cities?q=` → autocompletado de ciudades (DISTINCT, LIKE)
   - `GET /api/lodgings/{id}/availability?checkIn=&checkOut=` → devuelve disponibilidad de fechas
   - `POST /api/reservations` → crear reserva (con validación de solapamiento y optimistic locking)
   - `GET /api/reservations/{id}` → detalle de reserva

3. **Seguridad**
   - `POST /api/reservations` requiere `@PreAuthorize("isAuthenticated()")`
   - Endpoints públicos: búsqueda, ciudades, disponibilidad

### Frontend

1. **Home.jsx** — Conectar el formulario de búsqueda existente:
   - Input de ciudad con autocompletado (debounce 300ms, fetch a `/cities?q=`)
   - Inputs de fecha check-in / check-out (`<input type="date">`)
   - Botón "Realizar búsqueda" → navega a `/search?city=...&checkIn=...&checkOut=...`

2. **Nueva página: SearchResults.jsx**
   - Ruta: `/search?city=&checkIn=&checkOut=`
   - Sidebar izquierdo: filtros (categoría, rango de precio, features)
   - Grilla derecha: cards de resultados (imagen, nombre, ciudad, precio/noche, puntuación)
   - Estado vacío: mensaje si no hay resultados
   - URL compartible con query params

3. **ProductDetail.jsx** — Agregar sección de disponibilidad:
   - Precio por noche + cálculo estimado
   - Componente Calendario (react-datepicker con fechs ocupadas deshabilitadas)
   - Botón "Reservar" para usuarios autenticados
   - Modal/formulario de confirmación de reserva

### Testing

- Backend: TDD estricto (tests antes del código)
  - Unitarios (Mockito) para servicios
  - Integración (MockMvc + Testcontainers) para endpoints
  - Test específico de solapamiento de fechas concurrente
- Frontend: smoke manual (no hay test runner en frontend)

## Decisiones técnicas

| Decisión | Opción elegida | Motivo |
|----------|---------------|--------|
| Calendario | react-datepicker | Librería liviana, soporta fechas deshabilitadas, personalizable |
| Concurrencia | `@Version` (optimistic locking) | Previene doble reserva sin overengineering |
| Precio | `pricePerNight` fijo en Lodging | Suficiente para MVP, temporada queda como deuda |
| Filtros | Categoría + precio + features | Datos ya modelados, sin agregar complejidad nueva |
| Búsqueda | Endpoint unificado con query params | RESTful, URL compartible, fácil de extender |

## Riesgos

| Riesgo | Mitigación |
|--------|-----------|
| Reservas concurrentes solapadas | `@Version` + manejo de OptimisticLockException en GlobalExceptionHandler |
| Calendario no soportado en browser | react-datepicker es cross-browser |
| Fechas inválidas (check-out <= check-in) | Validación en DTO con `@AssertTrue` y en frontend antes de enviar |
| Migración de schema con datos existentes | `ddl-auto=update` agrega columnas sin perder datos |

## Proximo paso

Pasar a especificación (spec) con los criterios de aceptación detallados.
