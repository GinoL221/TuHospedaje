# Guion de Demostración Académica — TuHospedaje

## Objetivo

Mostrar de punta a punta los recorridos definidos en [product.md](../../../product.md): exploración y búsqueda, autenticación con roles, favoritos, reserva e historial/cancelación, y administración del catálogo.

La demostración usa un entorno local descartable. El hardening operativo de producción queda fuera del recorrido académico.

## Preparación

1. Confirmar que el entorno usa la rama `admin-dashboard-counts` y el commit publicado `a15194c`.
2. Crear la base demo y cargar el seed siguiendo la sección **Datos de demo** del [README](../../../README.md).
3. Verificar que las imágenes canónicas estén disponibles en el directorio externo configurado. Si no están, usar los placeholders y omitir el escenario visual de imágenes canónicas.
4. Iniciar el backend con el perfil `dev` en `http://localhost:8080`.
5. Iniciar el frontend en `http://localhost:5173`.
6. Abrir una ventana incógnito para el recorrido de huésped y otra ventana normal para el administrador.

El usuario administrador de demo es `admin@tuhospedaje.com` con la credencial dev-only documentada en el README. Crear un usuario huésped nuevo durante la demo o usar uno creado previamente en la base descartable.

## Recorrido recomendado

### 1. Catálogo y búsqueda — huésped anónimo

- Abrir la página inicial.
- Mostrar las tarjetas de alojamientos y el formulario de búsqueda.
- Buscar por ciudad, fechas y cantidad de huéspedes.
- Abrir un resultado y mostrar nombre, ubicación, precio, descripción, características, políticas, galería y disponibilidad.
- Intentar reservar sin sesión: el flujo debe llevar a iniciar sesión.

**Evidencia:** `product.md`, TC-30, TC-31, TC-40 y TC-41.

### 2. Registro, autenticación y favoritos — huésped

- Crear una cuenta desde **Crear cuenta**.
- Iniciar sesión con la cuenta creada.
- Mostrar en el encabezado el usuario autenticado, **Favoritos**, **Mis reservas** y **Cerrar sesión**.
- Agregar el alojamiento seleccionado a favoritos y abrir la sección **Favoritos**.
- Recargar la página para mostrar que la sesión persiste mediante cookies.

Como evidencia técnica opcional, mostrar en las herramientas del navegador que `ACCESS_TOKEN` es `HttpOnly` y que las mutaciones usan el token CSRF. No mostrar valores sensibles en una grabación o presentación.

**Evidencia:** TC-42 y TC-43.

### 3. Reserva y confirmación — huésped autenticado

- Volver al detalle del alojamiento.
- Elegir un rango futuro disponible; las fechas ocupadas deben aparecer deshabilitadas.
- Hacer click en **Reservar**.
- Confirmar que el formulario muestra los datos del alojamiento, los datos del huésped, las fechas y el total calculado.
- Completar el teléfono y confirmar la reserva.
- Mostrar la pantalla de confirmación con alojamiento, fechas, huésped, total y aviso de email.
- Seguir **Ver mis reservas**.

Para el entorno local, si SMTP está deshabilitado, presentar el log de `ConsoleEmailServiceImpl` como evidencia de solicitud de envío. No afirmar que el email llegó a un buzón sin Mailtrap configurado.

**Evidencia:** TC-30, TC-31, TC-32 y TC-35.

### 4. Historial y cancelación — huésped autenticado

- Abrir **Mis reservas**.
- Mostrar nombre del alojamiento, ciudad, fechas, noches, contacto, número de reserva, total y estado `CONFIRMED`.
- Hacer click en **Cancelar reserva**.
- Confirmar la operación en el diálogo.
- Verificar que el estado cambia a `CANCELLED` y que la acción de cancelación deja de aparecer.
- Si el tiempo lo permite, repetir sobre la misma reserva para mostrar la idempotencia.

**Evidencia:** TC-33, TC-44 y TC-47.

### 5. Administración del catálogo — administrador

- Cerrar la sesión del huésped e iniciar sesión con el administrador demo.
- Abrir el panel desde el nombre del administrador en el encabezado.
- Mostrar el dashboard con el total de reservas y las reservas recientes.
- Recorrer las pestañas de categorías, características, políticas, alojamientos, reservas y usuarios.
- Abrir la edición de un alojamiento y mostrar los campos precargados.
- Para evidenciar CRUD sin dejar basura, crear un registro de prueba en una entidad simple, editarlo y eliminarlo dentro de la misma sesión.
- Cerrar sesión y volver a la página pública.

No usar una base productiva ni credenciales reales para este recorrido.

**Evidencia:** TC-36, TC-37, TC-38 y TC-39.

### 6. Cierre responsive y evidencia automatizada

- Abrir DevTools con un viewport móvil de `390x844`.
- Mostrar el menú móvil, la navegación y el recorrido de **Mis reservas** sin overflow horizontal.
- Mostrar brevemente el plan de pruebas y los resultados automatizados.
- Resaltar que la suite cubre backend, frontend, Chromium, Firefox y mobile Chromium.

**Evidencia:** TC-45, TC-46 y TC-47; resumen de ejecución de `sprint-4-test-plan.md`.

## Evidencia de CI

El workflow actual ejecuta la matriz completa para pull requests y para pushes a `main` o `sprint-4`. Un push directo a `admin-dashboard-counts` no dispara ese workflow por su filtro de ramas. Para obtener evidencia remota de esta rama, abrir un pull request y conservar el enlace a la ejecución y sus artefactos de Playwright.

La ejecución debe mostrar cinco jobs: backend, frontend, E2E Chromium, E2E Firefox y E2E mobile Chromium. Los artefactos de Playwright se conservan durante siete días.

## Criterio de cierre

La demo está completa cuando se observan estos cuatro resultados:

- catálogo y búsqueda funcionando;
- registro o login con roles diferenciados;
- reserva, consulta y cancelación por parte del huésped;
- administración del catálogo con permisos de administrador.

Los escenarios de imágenes canónicas sin sus JPEG maestros externos, SMTP sin Mailtrap y hardening productivo no bloquean el recorrido académico si quedan documentados como condiciones del entorno.
