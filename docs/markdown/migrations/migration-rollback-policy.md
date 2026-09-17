# Política de rollback de migraciones

Esta política aplica a los releases que cambian el esquema de MariaDB. Complementa el [runbook de backup y restore](../../../ops/mariadb/README.md).

## Propiedad e invariantes

- Flyway es el único dueño de cambios de esquema y ejecuta `classpath:db/migration`; Hibernate usa `ddl-auto=validate` y no crea ni modifica tablas.
- La aplicación usa `DB_USERNAME`/`DB_PASSWORD` sin permisos DDL. Flyway usa la cuenta separada `DB_MIGRATION_USERNAME`/`DB_MIGRATION_PASSWORD` en cada inicio o reinicio.
- Una migración registrada como aplicada es inmutable: no se edita, borra ni se altera manualmente `flyway_schema_history`. Un cambio se expresa en una nueva migración versionada.
- No se usa `baseline-on-migrate`, `repair` ni reintentos ciegos como respuesta a un incidente.

## Diseño para poder volver atrás

Toda migración productiva sigue expandir/contraer:

1. **Expandir:** agregar estructuras aditivas y compatibles (por ejemplo, columna nullable, tabla o índice). Antes de desplegar, definir y registrar la ventana en la que el esquema expandido admite la imagen anterior y la nueva.
2. **Transición:** desplegar una aplicación que lea ambos formatos, complete el backfill y, cuando corresponda, haga dual-write. Mantener explícitamente la ventana `app anterior + esquema nuevo` y `app nueva + esquema nuevo`; no retirar la imagen anterior hasta verificar ambas.
3. **Contraer:** quitar la compatibilidad, restricciones o estructuras antiguas solo después de que no queden instancias de la app anterior, el backfill esté verificado y exista un punto de recuperación. Una contracción cierra el rollback por imagen hacia la versión anterior.

Las migraciones destructivas o que no permitan esas ventanas requieren una ventana de mantenimiento y un plan de restore/cutover aprobado antes del release.

## Puntos de recuperación y decisión

Antes de migrar, registrar el digest de ambas imágenes, versión y checksum de Flyway, estado de `flyway_schema_history`, backup elegido con URI/checksum/metadatos y evidencia de sus verificaciones. Conservar logs de migración, hora de corte, responsable y resultados de health checks. El backup operativo tiene RPO de 24 horas, RTO de 4 horas, retención de 30 días, cifrado y verificación mensual de restore aislado.

Elegir una sola ruta según la evidencia:

| Situación | Ruta | Condición de salida |
| --- | --- | --- |
| Falla de aplicación; esquema expandido sigue siendo compatible con la imagen anterior; no hubo contracción | **Rollback de imagen** al digest registrado | La imagen anterior inicia, Hibernate valida y los checks funcionales acordados pasan contra el esquema actual. |
| Hay que corregir el esquema o preservar datos posteriores al backup | **Migración correctiva hacia adelante** | Nueva migración versionada, probada desde el historial afectado y compatible con la app que queda activa. |
| Corrupción, esquema parcial no reconciliable, o la imagen anterior no es compatible; se acepta pérdida hasta el backup | **Restore verificado y cutover** | Restaurar en target nuevo/aislado, verificar datos, Flyway y aplicación; bloquear escrituras, cortar tráfico y recién entonces promover. |

Un restore puede perder hasta 24 horas de escrituras (RPO) y consume parte del objetivo de 4 horas (RTO). El cutover cambia el origen de datos: las escrituras hechas después del backup no reaparecen por volver a desplegar una imagen. No reabrir tráfico ni escrituras hasta registrar la reconciliación o la pérdida aceptada.

## Fallo parcial de DDL en MariaDB

MariaDB puede confirmar sentencias DDL individuales aunque la migración falle. Ante ese caso: detener el rollout y las escrituras, preservar el error y el historial Flyway, inventariar el esquema real y comparar cada sentencia con la migración. No editar la migración aplicada ni ejecutar `repair`. Solo se permite reintentar una migración si su recuperación de DDL parcial fue diseñada y verificada para esa versión; de otro modo elegir migración correctiva o restore/cutover según la tabla anterior. Ensayar la ruta elegida en un target aislado antes de producción.

## Matriz de verificación

| Verificación | Evidencia actual | Requisito antes del release con cambio de esquema |
| --- | --- | --- |
| Cadena nueva de Flyway y arranque | Existe `DatabaseMigrationIntegrationTest` | Ejecutar en la versión candidata. |
| Backfill de datos heredados | Existe cobertura para V3 | Ejecutar o ampliar para los datos y la migración candidata. |
| Inmutabilidad de checksum | Existe cobertura | Ejecutar en la versión candidata. |
| Validación Hibernate/esquema | Existe cobertura | Ejecutar en la versión candidata. |
| DDL parcial | Existe cobertura de recuperación para V2; no generaliza a otras migraciones | Probar el punto de fallo de cada migración con DDL múltiple o documentar por qué no aplica. |
| App anterior + esquema expandido y app nueva + esquema expandido | No ejecutado | Ejecutar ambas combinaciones durante la ventana de transición. |
| Restore + compatibilidad de aplicación y cutover | El runbook verifica restore aislado mensual; esta combinación no está ejecutada | Restaurar el backup elegido, iniciar la imagen objetivo, verificar health y flujo funcional, y ensayar el cutover. |

No presentar las dos últimas filas como evidencia existente hasta que se guarden sus resultados junto al release.
