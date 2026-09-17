# Auditoría del estado actual de TuHospedaje

> **Fecha:** 2026-09-16
> **Línea base:** `origin/main` en `14264d71263486b04074e4611ec7b0f60b693191`
> **Alcance:** auditoría de solo lectura previa a la definición de la próxima funcionalidad.

## Estado ejecutivo

- **Salud general:** Amarillo
- **Preparación para una versión:** No está listo

La línea base tiene capas Spring claras, manejo HTTP centralizado en el frontend, controles de autenticación sólidos, migraciones Flyway, entrega persistente de emails y una cobertura automatizada amplia. La seguridad operativa de producción está incompleta porque la procedencia de imágenes, los backups, la restauración, el rollback compatible con la base, el borde TLS y la automatización de despliegue no tienen procedimientos exigibles. La validación de búsqueda pública, el manejo de uploads, los límites de tiempo para integraciones externas y algunos flujos administrativos necesitan unidades de trabajo acotadas antes de sumar una funcionalidad grande.

## Evidencia

| Área | Hallazgo | Evidencia | Confianza |
| ------ | ---------- | ----------- | ----------- |
| Línea base | `origin/main` apunta al commit solicitado y el worktree separado usado para auditarlo está limpio. | `git rev-parse origin/main`; `git status --short` vacío antes y después de las verificaciones. | Confirmado — Alta |
| Estado Git | El worktree principal está en `admin-dashboard-counts`, commit `1a4e4e0`, 29 commits detrás y 7 por delante de la línea base. Sólo tiene `.mcp.json`, `.pi/` y `test-results/` sin seguimiento. | `git status --short --branch`; `git rev-list --left-right --count`. | Confirmado — Alta |
| Estado Git | La rama actual cambia 26 archivos, con 1.120 líneas agregadas y 84 eliminadas. Es demasiado grande para una sola unidad de revisión. | `git diff --stat 14264d7...admin-dashboard-counts`. | Confirmado — Alta |
| Estado Git | Hay 42 ramas locales; 22 ya están completamente integradas en la línea base. La mayoría de las ramas remotas restantes son equivalentes por parche a cambios ya integrados. | `git merge-base --is-ancestor`; `git cherry`. | Confirmado — Alta |
| Worktrees | Hay 22 worktrees registrados: el principal, uno limpio en la línea base y 20 worktrees separados bajo `.git/gentle-ai/candidate-views/` con cambios. Git no informa ninguno como podable. | `git worktree list --porcelain`; `git worktree prune --dry-run --verbose`. | Estado: Confirmado — Alta; vigencia: Desconocida |
| Arquitectura | El backend sigue Controller → Service interfaz/implementación → Repository y usa DTOs en los límites HTTP. | `ReservationController.java:35-40`; `ReservationService.java:10-22`; `ReservationServiceImpl.java:35-51`. | Confirmado — Alta |
| Arquitectura | El frontend centraliza solicitudes HTTP normales, credenciales, CSRF, refresh coordinado y un timeout de 15 segundos. | `frontend/src/services/api.js:7-8,56-113`; `refreshCoordinator.js:7-25`. | Confirmado — Alta |
| Arquitectura | Algunas páginas administrativas todavía conocen rutas y verbos HTTP literales en vez de consumir servicios de dominio. | `AdminFeatures.jsx:1-3`; `AdminUsers.jsx:1-3`. | Hecho confirmado; impacto inferido — Media |
| Autenticación | Los JWT se leen desde cookies HTTP-only, se validan contra el usuario actual y se rechazan si la cuenta está deshabilitada. | `JwtAuthenticationFilter.java:41-74`; `JwtService.java:29-36,59-69`. | Confirmado — Alta |
| Autorización | Las operaciones sensibles de reservas, usuarios y uploads tienen autorización a nivel de método. | `ReservationController.java:43-44,85-86,119-120`; `UserController.java:49,64,84`; `UploadController.java:31-32`. | Confirmado — Alta |
| CSRF y cookies | CSRF usa intercambio cookie/header. Las cookies de acceso y refresh son HTTP-only y permiten configurar `Secure` y `SameSite`. Refresh está excluido de CSRF de forma deliberada. | `SecurityConfig.java:50-58`; `AuthCookieFactory.java:28-32`; `RefreshCookieFactory.java:30-34`; `api.js:73-81`. | Controles: Confirmados; abuso desde mismo sitio: Sospechado — Media |
| CORS | CORS permite credenciales, pero restringe orígenes mediante configuración. No se inspeccionaron los valores efectivos de producción. | `SecurityConfig.java:130-133`; `CorsProperties.java:9`. | Implementación: Confirmada; despliegue: Desconocido |
| Uploads | La validación confía en el MIME declarado por el cliente y no verifica firmas ni decodifica la imagen antes de subirla a Cloudinary. | `CloudinaryServiceImpl.java:37,54-59,70`. | Confirmado — Alta |
| Seguridad del navegador | Nginx de producción no muestra una CSP, `X-Content-Type-Options`, `Referrer-Policy` ni política contra framing. | `frontend/docker/nginx/default.conf:7-42`. | Confirmado — Alta |
| Exposición de API | Swagger/OpenAPI está permitido públicamente. | `SecurityConfig.java:85`. | Confirmado — Alta |
| Corrección | Disponibilidad puede responder para un alojamiento inexistente porque consulta reservas por ID sin cargar primero el alojamiento, aunque documenta una respuesta 404. | `LodgingController.java:191-201`; `LodgingServiceImpl.java:424-443`. | Confirmado — Alta |
| Corrección | La búsqueda pública no limita el tamaño máximo de página ni valida claramente el orden de fechas, huéspedes y rango de precios antes de armar la consulta. | `LodgingController.java:168-179`; `LodgingServiceImpl.java:357-396`. | Confirmado — Alta |
| Confiabilidad | El upload del frontend evita el cliente HTTP compartido y no tiene una cancelación por timeout. | `ImageUpload.jsx:39-45`; `services/api.js:16-17,56-70`. | Confirmado — Alta |
| Confiabilidad | El cliente Cloudinary no tiene timeouts explícitos observables en el repositorio. No se verificaron los valores por defecto de la biblioteca. | `CloudinaryConfig.java:25`; `CloudinaryServiceImpl.java:34-40`. | Configuración ausente: Confirmada; impacto: Sospechado |
| Confiabilidad | El scheduler del outbox no aísla excepciones inesperadas de cada dispatcher; sólo captura errores del cleanup. | `EmailOutboxScheduler.java:25-47`; `EmailOutboxDispatcher.java:50-60`. | Confirmado — Alta |
| Confiabilidad | La entrega de email es intencionalmente “al menos una vez”; perder el lease después de la aceptación SMTP puede producir duplicados. | `EmailOutboxDispatcher.java:50-56`; `EmailOutboxDispatcherTest.java:174-188`. | Confirmado — Alta |
| Concurrencia | La protección contra doble reserva depende de los locks de MariaDB/InnoDB para una consulta de solapamiento vacía. Tiene una prueba real con MariaDB 10.11, pero depende del motor y aislamiento. | `ReservationRepository.java:24-35`; `ReservationConcurrencyTest.java:133-194`. | Hecho: Alta; riesgo de portabilidad: Media |
| Migraciones | Flyway controla el esquema, Hibernate valida el mapeo y producción separa credenciales de ejecución y migración. | `application.properties:34-38`; `application-prod.properties:4-7`. | Confirmado — Alta |
| Pruebas | Hay suites amplias de backend, frontend y Playwright. CI ejecuta Maven verify, cobertura frontend y tres proyectos de navegador. | Inventario de tests; `.github/workflows/ci.yml:15-36,66-180`. | Confirmado — Alta |
| Pruebas | Las integraciones backend usan Testcontainers y Playwright necesita backend, frontend y base en ejecución. No se corrieron bajo las restricciones de esta auditoría. | `TestcontainersConfiguration.java:10-18`; `playwright.config.js:6-18`. | Confirmado — Alta |
| Pruebas | Algunas verificaciones E2E de administración convierten una caída del panel en un skip exitoso. | `e2e/tests/admin-smoke.spec.js:25-29,51-56,76-81`. | Confirmado — Alta |
| Dependencias | Las auditorías de lockfiles npm informaron cero vulnerabilidades para frontend y E2E. El estado de advisories del backend sigue desconocido. | `npm audit --package-lock-only --ignore-scripts`, código 0 en ambos directorios. | npm: Confirmado al auditar; backend: Desconocido |
| Despliegue | Compose de producción acepta referencias de imagen, pero también permite builds locales. Esto contradice la afirmación de imágenes inmutables del README. | `compose.prod.yaml:42-45,79-83`; `README.md:104-120`. | Confirmado — Alta |
| CI/CD | CI prueba el código, pero no construye, escanea, publica, firma ni promueve imágenes; tampoco valida Compose de producción. | `.github/workflows/ci.yml`. | Confirmado — Alta |
| Persistencia | MariaDB usa un volumen persistente, pero no hay un proceso ejecutable de backup, retención ni restauración. | `compose.prod.yaml:8,98-100`; `README.md:142-148`. | Confirmado — Alta |
| Rollback | Volver a una imagen anterior no revierte migraciones Flyway y no hay una política documentada de compatibilidad expand/contract. | `README.md:148`; `application-prod.properties:2-7`. | Confirmado — Alta |
| TLS | La terminación TLS es externa, pero no hay proxy de referencia, política de saneamiento de headers, procedimiento de certificados ni definición de HSTS. | `README.md:104-106,132-142`; `frontend/docker/nginx/default.conf:19-25`. | Ausencia en repositorio: Confirmada; infraestructura real: Desconocida |
| Documentación | El README dice que Compose de producción llegará después y, más adelante, documenta un flujo de producción existente. | `README.md:49,104-148`. | Confirmado — Alta |
| Producto | El panel administrativo bloquea dispositivos táctiles con ancho de hasta 1024 px. | `frontend/src/pages/Admin/Admin.jsx:41-65`. | Confirmado — Alta |
| Producto | La administración de usuarios obtiene la lista completa y pagina en el navegador. | `UserController.java:41-52`; `UserServiceImpl.java:37-43`; `AdminUsers.jsx:9-26`. | Confirmado — Alta |
| Producto | El ciclo de reservas sólo tiene estados confirmado y cancelado. No se encontraron pagos, modificación, reembolso ni check-in/out. | `ReservationStatus.java:3-6`; `ReservationController.java:43-138`. | Estados: Confirmados; alcance futuro: Desconocido |

## Riesgos confirmados

### 1. La recuperación de producción no está definida de forma operativa

- **Severidad:** Alta
- **Impacto:** No se puede demostrar que los datos de reservas y usuarios cumplan un objetivo de recuperación. Una pérdida de volumen, error operativo o migración fallida puede ser irrecuperable.
- **Evidencia:** `compose.prod.yaml:8,98-100` aporta persistencia; `README.md:142-148` delega backup y restauración sin comandos, retención, RPO, RTO ni evidencia de pruebas.
- **Mitigación recomendada:** Agregar un runbook versionado, backup consistente, almacenamiento cifrado, retención y verificación de restauración aislada.
- **Esfuerzo estimado:** Medio, 2–4 días más una prueba de restauración ejecutada por mantenimiento.

### 2. Los artefactos de producción no son inmutables ni se promueven desde CI

- **Severidad:** Alta
- **Impacto:** Dos despliegues del mismo commit pueden producir imágenes diferentes y el contenido desplegado no tiene una cadena de procedencia exigible.
- **Evidencia:** `compose.prod.yaml:42-45,79-83` permite builds locales; `.github/workflows/ci.yml` no construye ni publica imágenes.
- **Mitigación recomendada:** Quitar `build` de producción, exigir referencias por digest, construir y escanear en CI y registrar los digests promovidos.
- **Esfuerzo estimado:** Medio, 3–5 días.

### 3. El rollback de migraciones de base está incompleto

- **Severidad:** Alta
- **Impacto:** Volver a una imagen anterior puede fallar o comportarse mal después de una migración incompatible.
- **Evidencia:** `README.md:148` indica que el rollback de imagen no revierte Flyway.
- **Mitigación recomendada:** Definir migraciones expand/contract, ventanas de compatibilidad, puntos de recuperación y criterios de rollback/restauración.
- **Esfuerzo estimado:** Medio, 2–4 días para política y verificaciones; el esfuerzo por migración varía.

### 4. El upload confía en metadatos enviados por el cliente

- **Severidad:** Media
- **Impacto:** Una cuenta administrativa comprometida puede enviar bytes arbitrarios etiquetados como imagen. El impacto final depende del procesamiento de Cloudinary.
- **Evidencia:** `CloudinaryServiceImpl.java:37,54-59,70`.
- **Mitigación recomendada:** Validar firmas, decodificar formatos permitidos, re-encodear cuando corresponda y rechazar contenido malformado antes de la integración externa.
- **Esfuerzo estimado:** Medio, 2–3 días con pruebas.

### 5. Los uploads no tienen deadlines de extremo a extremo

- **Severidad:** Media
- **Impacto:** Una llamada lenta a Cloudinary puede retener threads del backend y dejar el frontend indefinidamente en estado de carga.
- **Evidencia:** `ImageUpload.jsx:39-45`; `CloudinaryConfig.java:25`; `CloudinaryServiceImpl.java:34-40`.
- **Mitigación recomendada:** Enviar uploads mediante un servicio frontend con timeout y configurar límites explícitos de conexión y lectura en Cloudinary.
- **Esfuerzo estimado:** Bajo–Medio, 1–2 días.

### 6. Búsqueda y disponibilidad aceptan solicitudes inseguras o engañosas

- **Severidad:** Media
- **Impacto:** Páginas demasiado grandes pueden generar consultas costosas; filtros inválidos producen resultados ambiguos y un alojamiento inexistente puede aparecer disponible.
- **Evidencia:** `LodgingController.java:168-201`; `LodgingServiceImpl.java:357-443`.
- **Mitigación recomendada:** Crear un contrato validado de búsqueda, limitar el tamaño de página, validar campos relacionados y verificar la existencia del alojamiento.
- **Esfuerzo estimado:** Bajo–Medio, 2–3 días.

### 7. El polling del outbox no aísla fallos inesperados de despacho

- **Severidad:** Media
- **Impacto:** Una excepción de ejecución o persistencia puede abortar el ciclo y demorar otros mensajes hasta el próximo poll.
- **Evidencia:** `EmailOutboxScheduler.java:25-47`; `EmailOutboxSchedulerTest.java:31-40`.
- **Mitigación recomendada:** Aislar cada dispatcher, mantener telemetría acotada y probar que el scheduler continúa después de un fallo inesperado.
- **Esfuerzo estimado:** Bajo, 1 día.

## Deuda técnica

### Corregir ahora

- Resolver la contradicción del README sobre Compose de producción.
- Agregar deadlines y validación de contenido al upload.
- Validar parámetros de búsqueda y disponibilidad de alojamientos inexistentes.
- Aislar fallos de dispatch en el scheduler del outbox.
- Dividir `admin-dashboard-counts` antes de revisarlo: el diff actual supera las 1.200 líneas.
- Establecer una prueba de backup y restauración antes de declarar producción lista.

### Programar

- Construir, escanear, publicar por digest y validar Compose desde CI.
- Agregar CSP y headers básicos de seguridad en el borde TLS correcto.
- Implementar paginación y filtros del lado servidor para usuarios administrativos.
- Habilitar flujos administrativos de consulta en móvil si producto lo requiere.
- Separar DTOs de reservas por audiencia si se exige minimizar PII.
- Agregar auditoría de dependencias backend y verificación de checksums.
- Reemplazar tags mutables de imágenes base por digests controlados.
- Endurecer los skips E2E y reducir dependencia de fechas y datos mutables.

### Aceptar

- La entrega de email “al menos una vez” puede producir duplicados excepcionales si producto acepta el comportamiento y existe monitoreo.
- TLS puede permanecer fuera de Compose si se documentan propietario, saneamiento de headers, certificados y monitoreo.
- Los worktrees separados de revisión pueden permanecer registrados mientras sigan activos; el repositorio no permite determinar su vigencia.

## Brechas de producto

| Prioridad | Brecha | Impacto para usuarios | Costo |
| ----------- | -------- | ----------------------- | ------- |
| 1 | Disponibilidad puede responder éxito para un alojamiento inexistente. | Alto: decisiones de reserva engañosas y semántica HTTP inconsistente. | Bajo |
| 2 | La búsqueda acepta filtros débiles y tamaños de página sin límite máximo. | Alto: resultados confusos y degradación por solicitudes costosas. | Bajo–Medio |
| 3 | La lista administrativa de usuarios es completa y se pagina en cliente. | Medio–Alto: el rendimiento cae con el crecimiento de cuentas. | Medio |
| 4 | Administración no está disponible en dispositivos táctiles de hasta 1024 px. | Medio: operadores no pueden resolver tareas urgentes desde móvil o tablet. | Medio; requiere decisión de alcance |
| 5 | Reservas termina en confirmado/cancelado. | Potencialmente alto: no hay pagos, modificación, reembolso, expiración ni estados operativos. | Alto; requiere definición de producto |
| 6 | Reservas reutiliza proyecciones amplias con PII entre audiencias. | Medio: exposición innecesaria a medida que se agreguen roles. | Medio |
| 7 | Cloudinary es requerido por el servicio, pero su bean depende de configuración condicional. | Medio: un entorno sin Cloudinary puede fallar al iniciar o exponer un flujo inutilizable. | Bajo–Medio |

## Hoja de ruta recomendada

### Unidad 1 — Establecer evidencia de backup y restauración

- **Título:** Agregar un runbook probado de backup y restauración
- **Por qué importa:** Los datos tienen persistencia, pero no un camino de recuperación demostrado.
- **Archivos o subsistemas:** `README.md`, `ops/mariadb/`, `deploy/`.
- **Criterios de aceptación:**
  - Documenta backup cifrado, retención, RPO, RTO, restauración y verificación.
  - Usa un destino aislado para restaurar.
  - Falla de forma segura sin apuntar al volumen productivo por defecto.
  - Registra una prueba exitosa ejecutada por mantenimiento.
- **Plan de verificación:** Validación de scripts, revisión contra MariaDB 10.11 y ejercicio aislado con autorización separada.
- **Riesgo:** Alto por el impacto operativo; los scripts deben evitar producción por defecto.
- **Tamaño esperado:** 150–300 líneas cambiadas.

### Unidad 2 — Endurecer búsqueda y disponibilidad públicas

- **Título:** Validar solicitudes de búsqueda y disponibilidad
- **Por qué importa:** Corrige respuestas 200 incorrectas y limita el costo de consultas públicas.
- **Archivos o subsistemas:** `LodgingController`, DTO/validador de búsqueda, `LodgingServiceImpl`, pruebas de controller y service.
- **Criterios de aceptación:**
  - Disponibilidad de un alojamiento inexistente devuelve 404.
  - El tamaño de página tiene un máximo documentado.
  - Fechas, huéspedes y precios inválidos devuelven 400.
  - Las búsquedas válidas conservan su contrato.
- **Plan de verificación:** Integraciones de controller, pruebas de service y regresión de conteo de consultas.
- **Riesgo:** Medio; clientes existentes pueden depender de parámetros permisivos.
- **Tamaño esperado:** 220–350 líneas cambiadas.

### Unidad 3 — Limitar y validar uploads

- **Título:** Agregar parsing verificado y deadlines al upload de imágenes
- **Por qué importa:** El flujo confía en MIME y puede quedar pendiente ante una integración lenta.
- **Archivos o subsistemas:** `CloudinaryServiceImpl`, `CloudinaryConfig`, excepciones de upload, `ImageUpload`, servicio frontend y pruebas.
- **Criterios de aceptación:**
  - Rechaza MIME falso, archivos malformados, demasiado grandes y formatos no soportados.
  - Las llamadas externas tienen deadlines explícitos.
  - El frontend cancela y muestra un error recuperable específico.
  - JPEG, PNG y WebP válidos mantienen su comportamiento.
- **Plan de verificación:** Fixtures de bytes malformados, simulaciones de timeout y pruebas frontend sin llamadas reales a Cloudinary.
- **Riesgo:** Medio; una validación estricta puede rechazar archivos antes aceptados.
- **Tamaño esperado:** 300–390 líneas cambiadas.

### Unidad 4 — Hacer trazables los artefactos de producción

- **Título:** Construir y promover imágenes inmutables
- **Por qué importa:** El artefacto desplegado debe ser reproducible y quedar ligado al código revisado.
- **Archivos o subsistemas:** `.github/workflows/`, `compose.prod.yaml`, Dockerfiles y documentación de despliegue.
- **Criterios de aceptación:**
  - CI construye ambas imágenes una sola vez.
  - Producción usa referencias por digest.
  - Compose productivo no contiene builds locales.
  - Imágenes y configuración reciben verificaciones automáticas.
- **Plan de verificación:** Lint del workflow, chequeos de Dockerfile, validación de Compose y despliegue de staging por digest.
- **Riesgo:** Medio–Alto; requiere decidir registro y manejo de credenciales.
- **Tamaño esperado:** 250–380 líneas cambiadas.

### Unidad 5 — Acotar las consultas administrativas de usuarios

- **Título:** Paginar y filtrar la administración de usuarios
- **Por qué importa:** El flujo actual transfiere todos los usuarios y crece de forma lineal.
- **Archivos o subsistemas:** `UserController`, service/repository, DTOs, `AdminUsers`, servicio frontend y pruebas.
- **Criterios de aceptación:**
  - El endpoint tiene paginación acotada y orden explícito.
  - La búsqueda opcional no expone PII adicional.
  - La UI cubre carga, vacío, error y navegación.
  - La autorización permanece en el servidor.
- **Plan de verificación:** Pruebas de repository, service, controller y componentes frontend.
- **Riesgo:** Medio; el contrato cambia en frontend y backend de forma coordinada.
- **Tamaño esperado:** 300–390 líneas cambiadas.

## Próximo issue sugerido

### Título

Validar los contratos públicos de búsqueda y disponibilidad de alojamientos

### Problema

La API pública acepta parámetros de búsqueda débilmente restringidos y puede responder disponibilidad para un ID de alojamiento inexistente. Esto produce comportamiento engañoso para usuarios y permite solicitudes de página innecesariamente costosas.

### Alcance

- Agregar un máximo al tamaño de página.
- Validar orden de fechas, huéspedes positivos y rangos de precio coherentes.
- Verificar la existencia del alojamiento antes de responder disponibilidad.
- Mantener formatos de respuesta válidos y autorización existente.
- Agregar pruebas backend enfocadas, sin rediseñar el frontend.

### Criterios de aceptación

- Disponibilidad para un alojamiento desconocido devuelve 404.
- Combinaciones inválidas devuelven 400 con el formato estándar de error.
- Las solicitudes que superan el máximo se rechazan o limitan según una única política documentada.
- Los escenarios válidos actuales siguen pasando.
- La documentación refleja las restricciones reales.

### Plan de verificación

- Ejecutar pruebas de integración del controller para parámetros válidos e inválidos.
- Ejecutar `LodgingServiceImplTest` y pruebas del contrato de disponibilidad.
- Ejecutar la regresión de conteo de consultas.
- Confirmar que no cambien otros formatos de respuesta.
- Mantener la unidad por debajo de 400 líneas revisadas.

## Preguntas abiertas

- ¿Qué RPO y RTO deben cumplir los backups de producción y quién es responsable de las pruebas de restauración?
- ¿Qué registro y entorno de despliegue deben administrar las imágenes firmadas o fijadas por digest?
- ¿Administración móvil forma parte del producto y qué operaciones deben estar disponibles primero?
- ¿Pagos, modificación, reembolsos, expiración y check-in/out forman parte del alcance previsto?
- ¿Administradores y propietarios pueden ver la misma PII de reservas o cada rol necesita una proyección más limitada?
- ¿El trabajo único de `admin-dashboard-counts` debe dividirse y rebasarse para revisión o descartarse a favor de trabajo nuevo desde `origin/main`?

## Verificaciones realizadas

- Se confirmó que la línea base coincide con `14264d71263486b04074e4611ec7b0f60b693191`.
- `git diff --check HEAD^ HEAD` no encontró errores de whitespace en el último commit de la línea base.
- `npm audit --package-lock-only --ignore-scripts` informó cero vulnerabilidades en `frontend` y `e2e`.
- El worktree de la línea base permaneció limpio antes y después de las verificaciones.
- No se ejecutaron suites que escribieran artefactos ni pruebas que requirieran Docker, servicios o credenciales.
