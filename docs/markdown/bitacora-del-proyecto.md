---
title: "Bitácora del Proyecto"
subtitle: "TuHospedaje — de la base del sistema a la versión 1.0.1"
author: "Equipo de Desarrollo"
date: "Noviembre 2026"
---

# BITÁCORA DEL PROYECTO — TUHOSPEDAJE

## 1. Cómo leer esta bitácora

Esta bitácora cuenta en orden cronológico cómo se construyó TuHospedaje: qué se entregó en cada etapa, qué se decidió y por qué, qué quedó fuera a propósito y qué no se comprobó. Cada hito cita su fuente (commit, pull request o documento) para que pueda verificarse.

- **Corte:** 2026-11-10. Todos los hitos hasta `main@c9d91d0` (incluyendo T1 a T13) están integrados. Esta entrega final cierra las tareas documentales previas al tag v1.0.1.
- **Alcance:** resume y enlaza los informes y planes de prueba de cada sprint. No los reemplaza ni redefine el alcance de [product.md](../../product.md).
- **Límite:** no afirma que el sistema esté desplegado ni verificado en producción. La sección 9 detalla qué falta comprobar.

## 2. Línea de tiempo

| Fecha | Hito | Evidencia |
| --- | --- | --- |
| 2026-05-18 | Inicio del repositorio con la base del Sprint 1 | commit `74cb8a2` |
| Mayo 2026 | Sprint 1 y Sprint 2 (documentados con fecha de mes) | `sprint-1-report`, `sprint-2-report` |
| 2026-06-04 | Sprint 3 integrado: búsqueda, galería, favoritos, íconos y rediseño del panel | commit `7c29cf7` |
| 2026-06-10 | Sprint 4 integrado: reservas, WhatsApp, email de confirmación y E2E | commit `3343017` |
| 2026-06-27 | Cierre del Sprint 4 con pruebas faltantes y documentación | commit `4671cb1` |
| 2026-07-23 | Rama `sprint-4` integrada en `main` | PR #36 (`8a3fd43`) |
| 2026-08-10 | Reconciliación documental del Sprint 4 | PR #76 (`fe9420f`) |
| 2026-11-04 | Rediseño visual integrado en `main` | PR #343 (`ab73d8e`) |
| 2026-11-07 | Release **v1.0.0** y cierre del plan de endurecimiento | PR #413, tag `v1.0.0` en `a3f90a6` |
| 2026-11-08 | Unificación de errores 401/403 y deuda técnica posterior al release | PR #415 a #423, `main@83cda57` |
| 2026-11-08 a 11-10 | Correcciones y entrega de v1.0.1 (T1 a T13) | PR #425 a #449, `main@c9d91d0` |

## 3. Sprint por sprint

Las 35 historias oficiales (#1 a #35) se reparten entre los cuatro sprints. Las historias #36 a #43 y la cancelación de reservas son incrementos posteriores y no integran ese conjunto oficial.

### Sprint 1 — Base del sistema (US #1 a #11)

Estructura responsive, catálogo público, galería, paginación y panel de administración de alojamientos. El plan de pruebas aprobó 11 de 11 casos y registró 37 tests automatizados de backend. Detalle: informe y plan del Sprint 1.

### Sprint 2 — Seguridad y organización (US #12 a #21)

Autenticación con JWT, roles, categorías y características. La US #19 (correo de confirmación de registro) es la única historia opcional y no evaluada. El plan documenta 9 casos (TC-12 a TC-20), declara el incremento aprobado y registra 49 tests automatizados (JUnit 5 y MockMvc) sin fallos.

### Sprint 3 — Búsqueda, favoritos y políticas (US #22 a #29)

Búsqueda por ciudad y fechas, disponibilidad, favoritos, políticas, compartir en redes, puntuaciones y eliminación de categorías. El plan registró 139 tests de backend en verde y 62 de 62 escenarios funcionales, más cinco mejoras complementarias (TC-30 a TC-34).

### Sprint 4 — Reservas (US #30 a #35)

Selección de fechas con bloqueo de días ocupados, detalle e inicio de reserva, confirmación, historial personal, botón de WhatsApp y correo de confirmación. En CI sobre `main` quedaron 422 de 422 tests de backend, 416 de 416 de frontend (53 archivos) y 45 escenarios E2E en 13 specs. El plan verificó 152 de 152 escenarios funcionales.

## 4. Plan de endurecimiento (P0 a P28)

Después del Sprint 4 se ejecutó un plan de 29 tareas en 10 fases (0 a 9) para dejar el producto listo para publicar. Cerró con el release v1.0.0. Fuente: `odd/tasks/v1-release-hardening.md`.

| Fase | Tareas | Qué entregó |
| --- | --- | --- |
| 0 | P0 | Subida local de imágenes (`LocalImageStorageService`) en lugar de Cloudinary, con validación de tipo real del archivo |
| 1 | P1 | Rate limit por IP real detrás de un proxy inverso |
| 2 | P2 a P4 | Revocación inmediata de JWT al cambiar la contraseña, empaquetado estable del backend y borrado seguro de alojamientos |
| 3 | P5 a P8 | Accesibilidad: contraste, teclado y ARIA, voseo homogéneo y tabla de administración operable en pantallas angostas |
| 4 | P9 a P11 | Continuidad de la experiencia: destino tras el login, fechas en la ficha y formato local de fechas |
| 5 | P12 a P17 | Integridad del backend: códigos 4xx correctos, unicidad de puntuaciones, zona horaria, bloqueo de reservas concurrentes, política de contraseñas y correos en español |
| 6 | P18 a P21 | CI e infraestructura: permisos mínimos, acciones fijadas por SHA, dependencias sin alertas altas y `.gitignore` saneado |
| 7 | P22 a P25 | Frontend: limpieza de CSS e imágenes sin uso, títulos dinámicos, landmarks semánticos, objetivos táctiles de 44×44 px y tokens del Manual v2.1 |
| 8 | P26 y P27 | Documentación: README con arranque desde cero y definición del proyecto actualizada |
| 9 | P28 | Versión 1.0.0 |

## 5. Release v1.0.0

El 2026-11-07 se creó el tag anotado `v1.0.0` sobre `a3f90a6` (PR #413). Las imágenes de backend y frontend se escanean con Trivy y se publican en GHCR desde CI. Los conteos de CI registrados en el seguimiento del proyecto para esa revisión son 780 tests de backend (1 omitido), 759 de frontend con 94,5 % de cobertura de líneas y E2E en Chromium, Firefox y móvil. Estos conteos se citan del registro; no se recalcularon para este documento.

## 6. Después de v1.0.0

### 6.1 Deuda técnica posterior al release (2026-11-08)

| PR | Cambio |
| --- | --- |
| #415 | Versiones del stack corregidas en README y definición del proyecto |
| #417 | Las respuestas 401 y 403 de Spring Security devuelven cuerpo JSON localizado |
| #419 | OpenAPI alineado con los códigos 401 y 403 reales |
| #421 | Guard de respuestas ya enviadas y prueba de token CSRF inválido |
| #423 | Estilos `.btn-cancel` centralizados y utilidades de contrato CSS compartidas |

Al final de esta etapa `main` quedó en `83cda57` (793 tests de backend con 1 omitido, 767 de frontend y E2E en los tres navegadores, según el registro del proyecto).

### 6.2 Correcciones de v1.0.1

Una revisión general de `main@83cda57` produjo la lista de correcciones y endurecimientos de v1.0.1. Las tareas T1 a T8 se integraron en `main@9ef737d` y la secuencia T9 a T13 llevó `main` a `c9d91d0`. Seguimiento: `odd/tasks/v1.0.1-pre-release-fixes.md`.

| Tarea | PR | Resultado |
| --- | --- | --- |
| T1 | #425 | La sesión se renueva al arrancar si `/auth/me` responde 401 |
| T2 | #427 | El límite de login por email (30 por minuto) queda por encima del límite por IP |
| T3 | #429 | El largo máximo de la URL de imagen (255) coincide con la columna de la base |
| T4 | #431 | Script y runbook de backup del volumen de uploads |
| T5 | #433 | Las variables de producción llegan al backend en `compose.prod.yaml` |
| T6 | #435 | Cabeceras de seguridad y CSP en el nginx del frontend |
| T7 | #437 | CI, Dockerfile y acciones migrados a Node 24 |
| T8 | #439 | Higiene previa al release: puertos en loopback, `HEAD` en GET públicos, configuración obsoleta eliminada |
| T9 | #445 | Versión 1.0.1 alineada en backend, frontend, e2e y OpenAPI |
| T10 | #448 | Toolchain reproducible de PDFs con manifiesto `SOURCES.sha256` |
| T11 | #446 | Claridad de estado: avisos de registro histórico y tabla configurado vs comprobado |
| T12 | #449 | Bitácora del proyecto y rediseño editorial de portadas y tablas |
| T13 | #447 | Cierre de reservas: pass-through de `TOMCAT_INTERNAL_PROXIES`, regex de assets en nginx y gitignore |

## 7. Decisiones

| Decisión | Motivo | Fuente |
| --- | --- | --- |
| Cloudinary reemplazado por almacenamiento local (#122) | Había credenciales expuestas en commits antiguos y la US #3 exige subida funcional sin depender de un servicio externo | P0 |
| Revocación de JWT con `token_version` | Un cambio de contraseña invalida de inmediato los tokens anteriores, sin esperar su expiración | P2, migración V12 |
| Bloqueo pesimista sobre `Lodging` al reservar | El control optimista de `Reservation` no frena inserciones concurrentes sobre las mismas fechas | P15 |
| **D2:** límite de login por email en 30 por minuto | Evita que un atacante bloquee la cuenta de otra persona; el límite por IP se alcanza antes | T2 |
| **D3:** largo máximo de URL de imagen en 255, sin migración V14 | Alinea la validación con la columna `VARCHAR(255)` sin tocar el esquema | T3 |
| **D6:** cabeceras estándar y CSP con `img-src 'self' data: blob: https:` | Protección base del navegador sin romper la carga de imágenes | T6 |
| **D7:** regex de assets en nginx y proxies en T13; `Category.imageUrl` diferido | Resuelve las reservas de despliegue sin introducir una migración V14 antes del release | T13 |
| H5 pospuesto a después de v1.0.1 | Cambiar el usuario del contenedor de desarrollo podía desestabilizar los permisos de los volúmenes locales | T8 |
| Política offline para autochequeos | Maven y Testcontainers no descargan ni sustituyen imágenes; es una regla de conveniencia, no un sandbox | `offline-safetyunit.md` |

Las decisiones que cambian contratos o comportamiento visible se consultaron antes de implementarse.

## 8. Qué quedó fuera a propósito

- **Alcance del producto:** pasarela de pagos, mensajería interna y mapas interactivos.
- **N6:** depende de la configuración del proxy TLS externo.
- **H5:** contenedor de desarrollo como root sobre `./backend`, pospuesto por la razón de la sección 7.
- **Migración V14 para `Category.imageUrl`:** en base es `VARCHAR(2048)` (`V6`) mientras `HttpsImageUrlValidator` valida hasta 255; se difiere para no alterar el esquema antes del corte.
- **N10 a N19, H7 y H8:** hallazgos menores y limpieza local, reservados para issues posteriores. El detalle de cada uno está en el seguimiento interno de la auditoría, no en este repositorio.

## 9. Límites conocidos

Esta tabla separa lo que el repositorio configura de lo que se comprobó en un entorno real.

| Control | Estado en el repositorio | Comprobado en producción |
| --- | --- | --- |
| TLS y reverse proxy | Fuera de Compose; el proxy debe conservar `Host` y enviar `X-Forwarded-Proto: https` | No comprobado |
| Entrega real de correo | Outbox y SMTP configurables, desactivados por defecto | No comprobado |
| Backups externos | Scripts y runbooks para MariaDB y uploads | No comprobado |
| Imágenes canónicas | Montaje local opcional; los JPEG maestros no están en el repositorio | No comprobado |
| Longitud URL de categoría | Base admite 2048 (`V6`), validador restringe a 255 sin migración V14 | Restringido por API, esquema diferido |

Los informes anteriores a v1.0.0, como `auditoria-estado-actual.md` y `current-main.md`, son registros históricos y no describen el estado actual.

## 10. Fuentes

- Informes y planes de prueba de los Sprints 1 a 4: `docs/markdown/sprint-{1..4}/`.
- Plan de endurecimiento: `odd/tasks/v1-release-hardening.md`.
- Correcciones de v1.0.1: `odd/tasks/v1.0.1-pre-release-fixes.md`.
- Historia de Git: commits y pull requests citados en cada tabla.
- Alcance oficial: [product.md](../../product.md) y la [matriz de trazabilidad](compliance/close-user-story-gaps-traceability.md).
