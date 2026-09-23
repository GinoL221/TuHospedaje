# Índice de documentación

Este archivo es un índice de navegación. No reemplaza las fuentes de autoridad, no redefine el alcance del producto y no convierte un informe histórico o una prueba puntual en evidencia actual.

## Cómo leer la documentación

Seguí este orden cuando necesites resolver una duda:

1. **Alcance académico y demostración:** [product.md](../product.md).
2. **Requisitos oficiales:** los PDFs originales de Digital House Sprint 1–4, suministrados fuera del repositorio. La reconciliación documentada está en [la matriz de trazabilidad](markdown/compliance/close-user-story-gaps-traceability.md).
3. **Identidad de marca:** [Manual de Identidad Visual v2.0](diseno/manual-identidad.md).
4. **Traducción técnica de diseño:** [DESIGN.md](../DESIGN.md), siempre subordinado al manual de identidad.
5. **Arquitectura y decisiones académicas:** [Definición del Proyecto](markdown/project-definition.md), verificando las secciones de estado actual.
6. **Implementación y evidencia:** código, tests, CI y documentos fechados, siempre ligados a una revisión, entorno y fecha concretos.

## Jerarquía de autoridad

| Prioridad | Fuente | Qué define | Qué no prueba por sí sola |
| ---: | --- | --- | --- |
| 1 | PDFs originales de Digital House Sprint 1–4 | User stories oficiales | El estado actual de la implementación |
| 2 | [product.md](../product.md) | Alcance académico, recorridos de demo y exclusiones | Requisitos externos no incluidos en el producto |
| 3 | [Manual de Identidad Visual v2.0](diseno/manual-identidad.md) | Marca, tipografía, color, activos, voz e identidad | Que una regla de marca esté implementada |
| 4 | [DESIGN.md](../DESIGN.md) | Traducción técnica, convenciones y estado de adopción | Cambiar una decisión normativa del manual |
| 5 | Código, configuración, tests y CI | Comportamiento observado para una revisión concreta | Estado de otra revisión, otro entorno o infraestructura externa |
| 6 | Informes, auditorías, matrices y planes fechados | Contexto y evidencia histórica | La verdad actual sin una verificación nueva |

## Documentación vigente y normativa

- [Alcance del producto y definición de terminado](../product.md)
- [Manual de identidad visual](diseno/manual-identidad.md)
- [Sistema técnico de diseño](../DESIGN.md)
- [Definición del proyecto, arquitectura y ADRs](markdown/project-definition.md)

Estos documentos deben enlazar a su autoridad superior cuando traducen una decisión. No dupliques aquí sus reglas: usá el índice para encontrarlas.

## Cumplimiento académico y evidencia

- [Matriz de trazabilidad US #1–#35](markdown/compliance/close-user-story-gaps-traceability.md)
- [Evidencia browser y CI](markdown/compliance/close-user-story-gaps-browser-evidence.md)
- [Guía de demo académica](markdown/sprint-4/academic-demo-guide.md)
- [Verificación manual del frontend](markdown/frontend-testing/frontend-testing-manual-verification.md)

La matriz conserva separados los requisitos oficiales y los incrementos posteriores. La evidencia browser/CI identifica su revisión y sus límites. La cobertura de un job no demuestra automáticamente cada criterio de una user story.

## Auditorías, seguridad y operación

- [Auditoría del estado actual](markdown/audits/auditoria-estado-actual.md)
- [Reporte de hardening de seguridad del backend](markdown/backend-security-hardening/backend-security-hardening-report.md)
- [Política de rollback de migraciones](markdown/migrations/migration-rollback-policy.md)
- [Migraciones Flyway](../backend/src/main/resources/db/migration/)

Las auditorías son fechadas. Antes de usar un hallazgo para cambiar el producto, verificá la revisión, el entorno y la vigencia del dato. La existencia de una migración, un volumen o un informe no demuestra por sí sola un backup/restauración operativa, un rollback probado ni preparación para producción.

## Sprints y documentos históricos

Los reportes y planes de Sprint describen cortes históricos del proyecto. Usalos para reconstruir decisiones y contexto, no como sustituto de `product.md`, de los PDFs originales o de la evidencia de una revisión actual.

- [Sprint 1 — reporte](markdown/sprint-1/sprint-1-report.md) · [test plan](markdown/sprint-1/sprint-1-test-plan.md)
- [Sprint 2 — reporte](markdown/sprint-2/sprint-2-report.md) · [test plan](markdown/sprint-2/sprint-2-test-plan.md)
- [Sprint 3 — reporte](markdown/sprint-3/sprint-3-report.md) · [test plan](markdown/sprint-3/sprint-3-test-plan.md)
- [Sprint 4 — reporte](markdown/sprint-4/sprint-4-report.md) · [test plan](markdown/sprint-4/sprint-4-test-plan.md)

## Entregables publicados en el repositorio

- [Definición del proyecto en PDF](entregables/project-definition.pdf)
- [Sprint 1 — reporte PDF](entregables/sprint-1-report.pdf) · [test plan PDF](entregables/sprint-1-test-plan.pdf)
- [Sprint 2 — reporte PDF](entregables/sprint-2-report.pdf) · [test plan PDF](entregables/sprint-2-test-plan.pdf)
- [Sprint 3 — reporte PDF](entregables/sprint-3-report.pdf) · [test plan PDF](entregables/sprint-3-test-plan.pdf)
- [Sprint 4 — reporte PDF](entregables/sprint-4-report.pdf) · [test plan PDF](entregables/sprint-4-test-plan.pdf)

Estos PDFs son artefactos de entrega y contexto. No sustituyen los PDFs originales de requisitos cuando se analiza la autoridad de una user story.

## Cómo citar evidencia actual

Una afirmación sobre implementación debe indicar, como mínimo:

- revisión o commit exacto;
- entorno y fecha de observación;
- test, job, archivo o procedimiento que la respalda;
- límite de lo que la evidencia no demuestra.

No afirmes desde este índice que existe entrega de email en Mailtrap, publicación de JPEG canónicos, backup/restauración de producción, soporte de dark mode, ejecución de proveedores externos o CI verde más allá de la revisión citada. No contiene secretos ni requiere leer `.env` para navegar la documentación.
