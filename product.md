# Producto: TuHospedaje

TuHospedaje es una plataforma web para descubrir y reservar alojamientos turísticos. Resuelve la necesidad de reunir la búsqueda, comparación y gestión de reservas en una experiencia clara para huéspedes y administradores.

## Fuente de verdad y límites de este documento

Los PDF originales de Digital House `Sprint 1.pdf` a `Sprint 4.pdf` definen las historias **#1–#35 y sus criterios de aceptación**. Este documento resume el producto; no elimina criterios ni cambia sus prioridades. Los planes y reportes del repositorio son documentos derivados, no sustitutos de esos originales.

La [matriz de trazabilidad](docs/markdown/compliance/close-user-story-gaps-traceability.md) conserva el detalle por historia, las fuentes y los límites de evidencia. [DESIGN.md](DESIGN.md) traduce la identidad visual a reglas técnicas; no modifica los requisitos funcionales de los PDF. La presencia de una función en estos recorridos no afirma que esté implementada o verificada en un SHA concreto.

## Usuarios objetivo

- **Huéspedes:** personas que buscan un alojamiento según destino, fechas y necesidades.
- **Administradores:** personas responsables de publicar y mantener la oferta de alojamientos.

## Recorridos principales

### Huésped

1. Explora el catálogo y busca alojamientos por ciudad y fechas.
2. Consulta el detalle, características, políticas y disponibilidad.
3. Se registra o inicia sesión, guarda favoritos y realiza una reserva.
4. Consulta su historial de reservas (#33).
5. Comparte alojamientos, puntúa después de una estadía, consulta por WhatsApp y recibe el correo de confirmación de reserva (#27, #28, #34 y #35), según los criterios de cada historia.

La cancelación es una mejora posterior al conjunto oficial #1–#35. No reemplaza la consulta del historial ni es un criterio original de Sprint 4.

### Administrador

1. Inicia sesión con permisos de administración.
2. Crea, actualiza y organiza alojamientos, categorías, características, políticas e imágenes.
3. Consulta la información necesaria para mantener el catálogo y las reservas.

## Alcance académico

El alcance oficial se organiza así; esta lista describe requisitos, no sprints certificados como terminados:

- **Sprint 1 (#1–#11):** estructura responsive del sitio, catálogo, galería, paginación y administración de alojamientos.
- **Sprint 2 (#12–#21):** autenticación, roles, categorías y características. **#19, correo de confirmación de registro, es la única historia explícitamente opcional y no evaluada.**
- **Sprint 3 (#22–#29):** búsqueda, disponibilidad, favoritos, políticas, compartir en redes, puntuaciones y eliminación de categorías.
- **Sprint 4 (#30–#35):** selección de fechas, detalle y realización de reserva, historial, WhatsApp y correo de confirmación de reserva.

Una prioridad baja no convierte una historia en opcional. #27, #28, #34 y #35 forman parte del alcance oficial evaluado. El envío o resultado de un proveedor externo requiere su propia evidencia; abrir un enlace o configurar SMTP no prueba una entrega.

## Alcance por dispositivo

La experiencia web del huésped incluye adaptación a diferentes dispositivos según los criterios de cada historia. Hay requisitos explícitos en #1, #2, #6, #7, #18, #20, #24, #25 y #34; #35 también exige un correo adecuado para diversos dispositivos. Esto no implica desarrollar una aplicación móvil nativa ni demuestra que cada vista ya esté verificada.

**Administración es la excepción:** #9 exige que el panel no sea responsive y muestre un mensaje de no disponibilidad al acceder desde móvil (Sprint 1, pp. 25–26). El breakpoint y la detección táctil actuales son convenciones de implementación, no valores fijados por el PDF.

Las diferencias entre convenciones actuales y requisitos, incluido el calendario doble y la ubicación móvil de WhatsApp, se registran como [pendientes de reconciliación](DESIGN.md#official-requirements-reconciliation). Documentarlas no las aprueba ni cambia la interfaz.

## Fuera de alcance

Esta entrega no incluye pasarelas de pago, chat interno ni mapas en vivo.

## Definición de terminado para la demostración

La demostración está terminada cuando permite mostrar, de punta a punta:

- la exploración y búsqueda de alojamientos;
- el registro o inicio de sesión con roles diferenciados;
- una reserva confirmada y su consulta en el historial del huésped;
- la administración del catálogo con permisos de administrador.

Estos cuatro recorridos organizan la demostración; no sustituyen la aceptación de las historias oficiales. El cierre académico exige contrastar todos los criterios evaluados #1–#35, salvo #19, con evidencia de la revisión objetivo. Cancelación y otros incrementos posteriores se muestran por separado y no reemplazan criterios oficiales.

El endurecimiento exclusivamente productivo, la operación a gran escala y las mejoras ajenas a los criterios oficiales son adicionales. Esta distinción no vuelve opcionales historias de prioridad baja ni sus integraciones externas.
