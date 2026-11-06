# v1.0.0 — endurecimiento previo a la entrega

Plan de correcciones aprobado tras la revisión amplia de `main` (`c806a4a`). Origen: informe de siete áreas (historias, backend, frontend, navegadores, secretos, documentación, entrega). Todo cambio va en su rama con issue aprobado y PR; el tag `v1.0.0` se crea recién al cerrar la Fase 9.

## Specs

- **S1.** No se taggea `v1.0.0` mientras exista un bloqueante abierto: credenciales expuestas, límite de peticiones que bloquea el sitio en producción, o build de imagen roto.
- **S2.** «Se debe poder subir una o más imágenes del producto.» — enunciado de la historia 3. La subida es parte del alcance y debe seguir funcionando sin Cloudinary.
- **S3.** Cloudinary se elimina del repositorio: dependencia, configuración, servicio, endpoints y variables de entorno. La cuenta del proveedor deja de ser necesaria para operar.
- **S4.** La validación de imágenes existente se conserva sin cambios de comportamiento: tipos permitidos JPEG/PNG/WebP/GIF, sin SVG, magic bytes verificados, 5 MB de tope, 8192 px por lado y 20 Mpx decodificados.
- **S5.** El nombre del archivo almacenado no se deriva del nombre enviado por el cliente: sin path traversal ni colisiones.
- **S6.** La subida sigue siendo solo para ADMIN y sigue protegida por CSRF.
- **S7.** La imagen servida debe ser accesible por GET sin autenticación, igual que hoy, para que las fichas de alojamiento rendericen en el sitio público.
- **S8.** Las pruebas se escriben primero: RED observado antes de la implementación, y toda la suite queda verde.
- **S9.** No se rompen las 35 historias de usuario ni los tests existentes.
- **S10.** Los secretos que estuvieron en el historial público se consideran comprometidos: la rotación o la eliminación del proveedor es parte del cierre, no del código.
- **S11.** Cada corrección cierra con commit de unidad de trabajo en su rama, con tests y documentación junto al comportamiento.
- **S12.** Las decisiones de producto que cambian contratos existentes (política de contraseñas, grilla de recomendaciones, título de pestaña) se consultan antes de implementarse.

## Tasks

| ID | Tarea | Specs | Fase | Rama |
| --- | --- | --- | --- | --- |
| P0 | Subida local de imágenes y eliminación de Cloudinary | S2–S7 | 0 | `fix/local-image-uploads` |
| P1 | Rate limit detrás de proxy (`forward-headers-strategy`) | S1 | 1 | `fix/rate-limit-real-client-ip` |
| P2 | Vida del access token y revocación por cambio de contraseña | S1 | 2 | `fix/access-token-revocation` |
| P3 | Configuración de producción: outbox, WhatsApp build-arg, jar del Dockerfile | S1, S8, S9, S11 | 2 | `fix/prod-delivery-config` |
| P4 | Borrado de alojamiento con datos asociados | S9, S8, S11, S12 | 2 | `fix/lodging-delete-with-references` |
| P5 | Contraste en reposo y hover de toda la interfaz | — | 3 | `fix/contrast-hover-states` |
| P6 | Foco, teclado y semántica de errores — en curso; foco visible, focus trap en UserProfileModal y semántica accesible de formularios | — | 3 | `fix/focus-keyboard-semantics` |
| P7 | Textos: voseo, mensajes de red, calendario y estados vacíos | — | 3 | `fix/ui-copy-locale` |
| P8 | Admin en ventana angosta sin pantalla táctil | — | 3 | `fix/admin-narrow-window` |
| P9 | Historias 4, 22 y 26 | S12, S9 | 4 | `fix/user-story-gaps` |
| P10 | Continuidad de flujo y detalles de UX | S9 | 4 | `fix/flow-continuity` |
| P11 | Fecha local en la reserva (`toISOString`) | S9 | 4 | `fix/booking-local-date` |
| P12 | Respuestas 4xx correctas y límites de paginación | — | 5 | `fix/error-status-mapping` |
| P13 | Migración: unicidad de ratings, versión y rango de fechas | S11 | 5 | `fix/integrity-migration` |
| P14 | Zona horaria de validación, URLs de imagen y versión en DTO | — | 5 | `fix/validation-details` |
| P15 | Endurecimiento: Swagger, secreto JWT, tope de body, bloqueo de reserva | — | 5 | `fix/backend-hardening` |
| P16 | Política de contraseñas | S12 | 5 | `feat/password-policy` |
| P17 | Correo de reserva en español con fecha y hora | S9 | 5 | `fix/reservation-email-copy` |
| P18 | CI: permisos, actions fijadas, reintento de publicación, tags | S1 | 6 | `ci/harden-workflow` |
| P19 | Dependencias con avisos de seguridad | — | 6 | `chore/dependency-security` |
| P20 | Compose y ejemplos de entorno | — | 6 | `chore/compose-hygiene` |
| P21 | Higiene del repositorio y `.gitignore` | — | 6 | `chore/repo-hygiene` |
| P22 | Código muerto del frontend | — | 7 | `chore/remove-dead-frontend-code` |
| P23 | Landmarks y títulos de página | S12 | 7 | `a11y/landmarks` |
| P24 | Objetivos táctiles y diálogos propios | — | 7 | `a11y/touch-targets` |
| P25 | Tokens de color y tipografía | — | 7 | `refactor/design-tokens` |
| P26 | README: arranque desde cero | — | 8 | `docs/readme-bootstrap` |
| P27 | Documentos desactualizados | — | 8 | `docs/stale-content` |
| P28 | Versión 1.0.0 | S1 | 9 | `chore/version-1.0.0` |

## Log

- **L1.** «bien, podes armar un plan para corregir todo, ordenado por prioridad»
- **L2.** «si lo apruebo / y arrancamos con la fase 0, cloudinary no lo usamos lo podemos borrar del repo»
- **L3.** «sigo tu recomendacion» — decisión: subida local en el backend en lugar de rotar credenciales de Cloudinary.
- **L4.** (evidencia, 2026-11-04) La base de desarrollo no contiene ninguna URL de Cloudinary: 190 imágenes de alojamiento apuntan a los assets canónicos del seed, y categorías y usuarios no tienen imágenes. Borrar la cuenta no rompe datos existentes.
- **L5.** (evidencia) El enunciado de la historia 3 exige subir imágenes, por lo que eliminar la subida sin reemplazo rompería el alcance.
- **L6.** (2026-11-05) PR #353 mergeado en `main`: corrección de contaminación en `AdminStatsControllerIntegrationTest` limpiando `email_outbox` antes de borrar usuarios.
- **L7.** (2026-11-05) PR #355 mergeado en `main` (`db5ef4b`), cerrando issue #354: subida local con `LocalImageStorageService`, validación en `ImageContentValidator`, endpoints públicos en `/uploads/**` y eliminación total de Cloudinary.
- **L8.** (2026-11-05) P1 implementado en rama `fix/rate-limit-real-client-ip`: `server.forward-headers-strategy=native` y `internal-proxies` configurados en `application-prod.properties` para aislar cuotas de rate limit por IP real detrás de nginx, verificado con `RemoteIpRateLimitIntegrationTest` y `UntrustedRemoteIpRateLimitIntegrationTest`.
- **L9.** (2026-11-05) P2 implementado en rama `fix/access-token-revocation`: `token_version` y `password_changed_at` en `User` (migración V12) para invalidar tokens viejos inmediatamente en `JwtAuthenticationFilter` tras cambio de contraseña, verificado con `PasswordChangeIntegrationTest` (401 inmediato para token previo) y TTL de 15m (900000 ms) en `application-prod.properties`.
- **L10.** (2026-11-05) PR #359 mergeado (`4ef1fa9`), issue #358 cerrado, seis jobs de CI verdes. P2 revoca tokens con `tokenVersion`; tokens legacy sin claim y revocación al logout no están cubiertos.
- **L11.** (2026-11-05) «si» — autorización para P3 (outbox, WhatsApp build-time, jar estable). «reintenta» — autorización para repetir el explorador fallido; el segundo intento completó el mapa. Ruta: implementación inline, sin unidades paralelas independientes; verificador independiente por riesgo de entrega (ítem 5). Sin reiniciar ni modificar el stack persistente.
- **L12.** (2026-11-05) Issue #360 creado (`type:bug`, `status:approved`) para P3. Implementación verificada localmente: `tools/tests/test_prod_delivery_config.py` (8/8 tests en la suite tools), 20/20 tests de outbox/SMTP, packaging de `target/app.jar` y build frontend con `VITE_WHATSAPP_NUMBER`. RDD approved y burned (`review-ae4b2623e2ecff55`).
- **L13.** (2026-11-05) PR #361 mergeado en `main` (`03ffb7f`), issue #360 cerrado automáticamente. P3 finalizado con éxito.
- **L14.** (2026-11-05) Decisión de producto para P4 aprobada por el usuario: bloquear borrado si el alojamiento tiene reservas asociadas (HTTP 400, mensaje claro) para proteger el historial y contratos de los huéspedes. Si no tiene reservas, limpiar automáticamente calificaciones (`ratings`) y favoritos (`user_favorites`) antes de eliminarlo. En el panel Admin (`AdminLodgings.jsx`), capturar el error y mostrarlo en una alerta accesible con `role="alert"`.
- **L15.** (2026-11-05) Issue #362 creado (`type:bug`, `status:approved`) para P4. Implementación y tests completados: `LodgingServiceImplTest` (14/14), `LodgingControllerIntegrationTest` (51/51), `AdminLodgings.test.jsx` (20/20). RDD assess medium.
- **L16.** (2026-11-05) PR #363 mergeado en `main` (`396e168`), issue #362 cerrado automáticamente. P4 y Fase 2 completadas.
- **L17.** (2026-11-05) Issue #364 creado (`type:bug`, `status:approved`) para P5. Implementación en rama `fix/contrast-hover-states`: hover de enlaces del header a `var(--secondary)` (9.19:1), botones de tablas de Admin (Editar a `var(--secondary)` 9.19:1, Eliminar y Quitar admin a `var(--feedback-danger-text)` 8.75:1, Hacer admin a `var(--feedback-success-text)` 6.56:1 con foco visible) y alerta de recomendaciones en Home a tokens semánticos de peligro (8.16:1). Tests de contrato de contraste añadidos y verdes (14/14 en `primaryActionContrast.test.js`). RDD assess medium.
- **L18.** (2026-11-05) PR #365 mergeado en `main` (`2c4a37a`), issue #364 cerrado automáticamente. P5 completado.
- **L19.** (2026-11-05) Issue #366 creado (`type:bug`, `status:approved`) para P6. Implementación en rama `fix/focus-keyboard-semantics`: foco visible en `ProductCard` (`--product-card-focus` en `.hotel-card-link`) e inputs del buscador (`Home.css`); focus trap, tecla `Escape` y foco inicial/restaurado en `UserProfileModal`; semántica de errores accesible (`aria-invalid`, `aria-describedby` y `role="alert"`) en `LoginPage` y `RegisterPage`. Tests unitarios en `UserProfileModal.test.jsx` (5/5), `LoginPage.test.jsx` (11/11) y `RegisterPage.test.jsx` (11/11). RDD assess medium.
