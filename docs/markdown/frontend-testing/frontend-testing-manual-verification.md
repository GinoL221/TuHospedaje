---
title: "Guía de Verificación Manual — Frontend Testing"
subtitle: "TuHospedaje — Confirmación end-to-end de los flujos caracterizados por la suite automatizada"
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
      <div>Frontend Testing — Verificación Manual — Junio 2026</div>
      <div>Página <span class="pageNumber"></span> de <span class="totalPages"></span></div>
    </div>
---

<style>
.page-break { page-break-before: always; }
table { width: 100%; } table, tr { page-break-inside: avoid; }
h1, h2, h3, h4 { page-break-after: avoid; }
</style>

# GUÍA DE VERIFICACIÓN MANUAL — FRONTEND TESTING

**Foco del Incremento:** Bootstrap de Vitest + React Testing Library en `/frontend` (0 → 173 tests, 22 archivos) y characterization testing de los flujos existentes.
**Cadena de PRs:** #12, #14, #15, #16, #17, #18, #19, #20, #21 (stacked-to-main) — change SDD `frontend-testing`, ya verificado y archivado.
**Propósito de esta guía:** confirmar a mano, en el navegador, que el comportamiento real de la app coincide con lo que la suite automatizada caracterizó. No es para encontrar bugs nuevos — es para validar que los tests no mienten.

## Cómo usar esta guía

1. Levantá el backend (`./mvnw spring-boot:run` desde `/backend`) y el frontend (`npm run dev` desde `/frontend`).
2. Recorré cada TC en orden — varios dependen de estado creado en uno anterior (ej. necesitás estar logueado para reservar).
3. Marcá la columna **Estado** de cada paso: `✔ Pasa`, `✘ Falla`, o `⚠ Distinto a lo esperado` (y anotá qué pasó en la sección de Notas al final del TC).
4. Los pasos marcados **(conocido)** corresponden a comportamiento que la suite automatizada ya documentó como hallazgo (gap de a11y, race condition, etc.) — no debería sorprenderte, solo confirmá que sigue siendo así.

---

## TC-FT01: Autenticación y persistencia de sesión

* **Cubre:** `AuthContext`, `LoginPage`, `RegisterPage`, `RequireAuth` (PRs #12, #15)
* **Precondiciones:** Backend activo. Usuario de prueba existente (o usar registro en el paso 2).

| Paso | Acción | Resultado Esperado | Estado |
|------|--------|---------------------|--------|
| 1 | Acceder a `/my-reservations` sin sesión activa | Redirige a `/login` | |
| 2 | Registrarse con un usuario nuevo desde `/register` | Checklist de complejidad de password se actualiza en vivo (✔/✘ por cada regla) mientras tipeás | |
| 3 | Completar registro exitoso | Login automático y redirección | |
| 4 | Cerrar sesión (logout) | Estado de auth se limpia, `localStorage` ya no tiene el token (verificable en DevTools → Application → Local Storage) | |
| 5 | Login con credenciales correctas | Redirección a la página desde la que veniste (si llegaste por un redirect de `RequireAuth`) o a home | |
| 6 | Login con credenciales incorrectas | Mensaje de error visible, sin navegar | |
| 7 | Recargar la página (F5) estando logueado | Sesión persiste, no te desloguea | |
| 8 | Editar manualmente el token en `localStorage` con un string inválido y recargar | **(conocido)** Sesión cae a estado "no logueado" en silencio, sin mensaje de error visible — el catch es silencioso por diseño actual | |

**Notas de este TC:**
si entro a un alojamiento no estando logueado me logueo desde el boton donde deberia estar el de reservas, cuando inicio sesion no vuelvo ahi me devuelve al /home
file:///home/ginopc/Imágenes/Capturas de pantalla/Captura de pantalla_20260619_021026.png cuando inicio sesion con un mail no registrado despues de anteriormente haber iniciado con otro mail me muestra esto (no probe si muestra lo mismo si anteriromente no me logue con otro user)
Los demas los probe y pasaron correctamente
---

## TC-FT02: Búsqueda y filtrado

* **Cubre:** `SearchResults`, `Pagination` (PR #17)
* **Precondiciones:** Al menos 2 categorías con alojamientos asociados en BD.

| Paso | Acción | Resultado Esperado | Estado |
|------|--------|---------------------|--------|
| 1 | Buscar una ciudad desde el home | Resultados cargan, un solo request a `/lodgings/search` | |
| 2 | Seleccionar 2+ categorías en el filtro | Filtrado aplica sin disparar un nuevo request (es client-side con 2+ categorías) | |
| 3 | Seleccionar UNA sola categoría | Sí dispara un nuevo request, ahora con `category` como query param (filtrado server-side) | |
| 4 | Quitar el chip de categoría | Resultados vuelven a incluir todas las categorías, un solo request adicional | |
| 5 | Quitar el chip de fecha | Resultados se recalculan sin el filtro de fecha | |
| 6 | Quitar el chip de precio | Resultados se recalculan sin el filtro de precio | |
| 7 | Buscar algo sin resultados | Empty-state visible, sin error en consola | |
| 8 | Navegar entre páginas con `Pagination` | Botones "Primera"/"Anterior" deshabilitados en la página 1, "Siguiente"/"Última" deshabilitados en la última | |
| 9 | Ver resultados sin estar logueado | No se dispara ningún request a `/favorites`, y ninguna card muestra el botón de favorito | |

**Notas de este TC:**
paso todos, con algunos arreglos que quiero hacerle a la busqueda y a los filtros pero por fuera de estos test
---

## TC-FT03: Detalle de alojamiento y reserva

* **Cubre:** `ProductDetail`, `BookingPage` (PRs #16, #18)
* **Precondiciones:** Usuario autenticado. Alojamiento sin reservas confirmadas en el rango de fechas a usar.

| Paso | Acción | Resultado Esperado | Estado |
|------|--------|---------------------|--------|
| 1 | Abrir el detalle de un alojamiento | Nombre, ciudad, precio, descripción, imagen y features visibles | |
| 2 | Seleccionar fechas en el calendario de `ProductDetail` | Botón de reserva se habilita solo con ambas fechas seleccionadas | |
| 3 | Intentar seleccionar una fecha ya ocupada (reserva CONFIRMED existente) | **(conocido — gap documentado)** Confirmá si el calendario la deshabilita visualmente; este flujo específico no quedó cubierto por la suite automatizada, es el primer lugar donde de verdad importa verificarlo a mano | |
| 4 | Navegar a `/booking/:id` con fechas precargadas | Total se calcula como `noches × precio por noche` | |
| 5 | Cambiar las fechas dentro de `BookingPage` | Total se recalcula en vivo | |
| 6 | Reservar con 1 sola noche | Texto dice "1 noche" (singular), no "1 noches" | |
| 7 | Completar reserva con datos válidos | Reserva creada, navegación a confirmación | |
| 8 | Intentar reservar fechas que se solapan con una reserva existente | Error visible, reserva no se crea | |
| 9 | Verificar el campo de teléfono en el formulario | **(conocido)** El `<label>` no está asociado al input vía `htmlFor`/`id` — funciona igual, pero un lector de pantalla no anunciaría el label correctamente | |

**Notas de este TC:**
no me deja pedir una reserva por una sola noche
9. no entendi bien, pero no me deja crear una reserva sin completar el telefono, si ya registre uno anteriormente toma ese y lo autocompleta en /booking/:id, comportamiento esperado entiendo

el resto ok
---

## TC-FT04: Favoritos

* **Cubre:** `ProductCard` (toggle), `FavoritesPage` (PRs #16, #17)
* **Precondiciones:** Usuario autenticado.

| Paso | Acción | Resultado Esperado | Estado |
|------|--------|---------------------|--------|
| 1 | Marcar un alojamiento como favorito desde una card de búsqueda | Ícono cambia inmediatamente (optimistic update) | |
| 2 | Click DOBLE y rápido en el mismo botón de favorito | **(conocido — race condition documentada)** Mirá si el estado final queda inconsistente con lo que muestra `/favorites` después — la suite documentó que no hay guard contra clicks concurrentes | |
| 3 | Provocar un fallo de red al marcar favorito (ej. cortar el backend un instante) | El ícono vuelve a su estado anterior (rollback visible) | |
| 4 | Ir a `/favorites` | Lista de favoritos coincide con lo marcado | |
| 5 | Quitar un favorito desde `/favorites` | Item desaparece de la lista | |
| 6 | Provocar un fallo de red al quitar un favorito desde `/favorites` | **(conocido — comportamiento distinto a `ProductCard`)** El item NO desaparece, pero tampoco aparece ningún mensaje de error visible — solo queda en consola | |

**Notas de este TC:**
3. no se si lo hice bien al test, pero bajando la base de datos no me deja agregar a favoritos
6. igual no me deja hacer nada cuando esta desconectada de la BD

el resto de los test los pasa
---

## TC-FT05: Mis reservas y reviews

* **Cubre:** `MyReservationsPage`, `ReviewsSection` (PR #18)
* **Precondiciones:** Usuario autenticado con al menos una reserva.

| Paso | Acción | Resultado Esperado | Estado |
|------|--------|---------------------|--------|
| 1 | Ir a `/my-reservations` | Lista de reservas propias, con noches/total calculados | |
| 2 | Confirmar que NO hay ninguna acción de cancelar reserva en esta pantalla | **(confirmado por diseño)** `MyReservationsPage` es de solo lectura hoy — si esperabas poder cancelar, es una funcionalidad que no existe todavía, no un bug | |
| 3 | Ir al detalle de un alojamiento con reviews | Lista de reviews visible | |
| 4 | Dejar una review nueva con 1 a 5 estrellas | Review se agrega a la lista | |
| 5 | Intentar enviar una review sin seleccionar estrellas | Validación impide el envío | |

**Notas de este TC:**
pasa los test, tendriamos que agregar una notificacion de confrimacion de que se va pisar la reseña
---

## TC-FT06: Admin — Usuarios y Alojamientos

* **Cubre:** `AdminUsers`, `AdminLodgings` (PR #19)
* **Precondiciones:** Usuario con rol ADMIN.

| Paso | Acción | Resultado Esperado | Estado |
|------|--------|---------------------|--------|
| 1 | Acceder al panel admin sin rol ADMIN | Acceso bloqueado | |
| 2 | Ver listado de usuarios | Roles visibles por usuario | |
| 3 | Cambiar el rol de OTRO usuario | Confirmación vía diálogo nativo del navegador (`window.confirm`), no un modal in-app | |
| 4 | Intentar cambiar tu propio rol de ADMIN | **(conocido — guard solo en frontend)** Botón deshabilitado para tu propia fila; no se verificó si el backend también lo bloquea si se llamara directo a la API | |
| 5 | Crear un alojamiento nuevo | Aparece en el listado | |
| 6 | Editar un alojamiento existente | Cambios se reflejan en el listado | |
| 7 | Eliminar un alojamiento | Confirmación vía diálogo **in-app** (`ConfirmDialog`), distinto al de usuarios | |
| 8 | Provocar un error al guardar un alojamiento (ej. nombre vacío) | Error inline visible debajo del campo (`.form-error`), sin `alert()` | |

**Notas de este TC:**
5. Crea el alojamiento pero no sube la imagen

el resto de los test los pasa
---

## TC-FT07: Admin — Categorías, Features, Políticas y Reservas

* **Cubre:** `AdminCategories`, `AdminFeatures`, `AdminPolicies`, `AdminReservations` (PRs #20, #21)
* **Precondiciones:** Usuario con rol ADMIN.

| Paso | Acción | Resultado Esperado | Estado |
|------|--------|---------------------|--------|
| 1 | Eliminar una categoría | Confirmación vía `ConfirmDialog` in-app (igual a Lodgings) | |
| 2 | Eliminar una feature | Confirmación vía `window.confirm` nativo (igual a Users) | |
| 3 | Eliminar una política | Confirmación vía `window.confirm` nativo (igual a Features) | |
| 4 | Notar la inconsistencia de los 3 pasos anteriores | **(conocido)** 3 mecanismos de confirmación distintos convivendo en 6 pantallas de Admin — no es un bug puntual, es deuda de UX a unificar en un change futuro si se decide | |
| 5 | Crear una política sin completar la descripción | Se guarda igual — `description` no es un campo obligatorio | |
| 6 | Ir a `/admin` → tab de Reservas | Lista de reservas de TODOS los usuarios, sin ninguna acción de editar/eliminar/cancelar | |
| 7 | Confirmar el estado de loading al entrar a la tab de Reservas | Mensaje "Cargando reservas..." visible brevemente antes de la lista | |

**Notas de este TC:**
5. no deberia ser obligatorio?
6. deberiamos agregarle funciones, pero el test lo pasa ok
7. carga demasiado rapido para verlo

file:///home/ginopc/Imágenes/Capturas de pantalla/Captura de pantalla_20260619_033551.png hay que mejorar esto, es complicado seleccionar uno
---

## Resumen final

| TC | Resultado |
|----|-----------|
| TC-FT01 — Autenticación y sesión | ⚠ 2 bugs nuevos (ver abajo) |
| TC-FT02 — Búsqueda y filtrado | ✔ Pasa |
| TC-FT03 — Detalle y reserva | ⚠ 1 bug nuevo (ver abajo) |
| TC-FT04 — Favoritos | ✔ Pasa (puntos 3/6 no probados correctamente, ver nota) |
| TC-FT05 — Mis reservas y reviews | ✔ Pasa, 1 sugerencia |
| TC-FT06 — Admin Usuarios/Alojamientos | ⚠ 1 bug nuevo (ver abajo) |
| TC-FT07 — Admin Categorías/Features/Políticas/Reservas | ✔ Pasa, 2 sugerencias |

**Bugs nuevos encontrados (no documentados por la suite — código de producción, no gap de testing):**

1. **Login no preserva el redirect de origen** (TC-FT01.5) — entrando a un alojamiento sin sesión y logueándose desde el botón donde debería estar "Reservar", al loguearse exitosamente vuelve a `/home` en vez de al alojamiento de origen. El flujo `location.state.from` no se preserva en este punto de entrada específico.
2. **Mensaje "Sesión expirada" engañoso en login fallido** (TC-FT01.6) — loguearse con un email no registrado, después de haber tenido una sesión previa con otro usuario, muestra el banner "Sesión expirada" en vez de un mensaje de credenciales inválidas. Probablemente el listener de `auth:unauthorized` se dispara donde no corresponde. Ver captura: `Captura de pantalla_20260619_021026.png`.
3. **No se puede reservar 1 sola noche** (TC-FT03.6) — el texto pluraliza bien ("1 noche"), pero el submit con ese rango de fechas no se deja completar. Hay una validación (frontend o backend) bloqueando el caso de 1 noche que el characterization test no llegó a ejercitar (solo verificó el texto, no el submit).
4. **La imagen no se sube al crear un alojamiento** (TC-FT06.5) — el alojamiento se crea correctamente pero sin imagen asociada.

**Sugerencias de producto/UX (no son bugs — comportamiento intencional o feature faltante):**

- Confirmación antes de pisar una review existente al reenviar (TC-FT05).
- Selector de íconos incómodo de usar en los modales de Admin — grid chico, requiere scroll. Ver captura: `Captura de pantalla_20260619_033551.png` (TC-FT07.4).
- ¿`description` debería ser obligatoria en Categorías/Políticas? — hoy es opcional por diseño, a confirmar si es lo esperado (TC-FT07.5).
- `AdminReservations` es de solo lectura hoy; si se espera poder operar sobre reservas desde Admin, es funcionalidad a agregar, no un bug (TC-FT07.6).

**Nota sobre TC-FT04 puntos 3 y 6:** cortar la BD entera no es la forma de probar el rollback de favoritos — con la BD caída, nada de la app funciona (no es específico de favoritos). Para probar el rollback real hay que bloquear solo esa request puntual desde DevTools → Network → clic derecho → "Block request URL", no tirar la base completa.
