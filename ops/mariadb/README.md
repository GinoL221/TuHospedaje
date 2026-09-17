# Backup y verificación de restore de MariaDB

Este runbook define los controles de recuperación de MariaDB para release readiness. Es genérico respecto del proveedor porque este repositorio no define la topología de producción.

## Política y responsables

- **RPO:** 24 horas. Operaciones programa un backup exitoso por día e investiga de inmediato cualquier ventana diaria incumplida.
- **RTO:** 4 horas. Los responsables de plataforma y base de datos mantienen procedimiento, acceso, capacidad y escalamiento para cumplir este objetivo.
- **Retención:** conservar los objetos diarios y su evidencia de checksum/metadatos durante 30 días. Configurar la política de ciclo de vida en object storage fuera de estos scripts y revisarla con el equipo de plataforma.
- **Almacenamiento:** usar object storage compatible con S3 y cifrado del lado del servidor con KMS. El equipo de plataforma administra creación, permisos, rotación y recuperación de las claves KMS; los operadores reciben el acceso mínimo para escribir o leer objetos de backup.
- **Ejercicio:** completar un restore aislado y completo cada mes. Guardar el JSON generado junto al registro de mantenimiento y avisar a los responsables de plataforma/base de datos ante cualquier check fallido.

Un backup fallido, evidencia de checksum ausente, validación de restore fallida, cadencia diaria incumplida o una previsión de restore superior al RTO de cuatro horas requiere escalamiento inmediato a los responsables de plataforma y base de datos. No declarar recuperación lista hasta repetir con éxito el punto fallido sobre un target aislado.

## Backup

`backup.sh` crea un dump lógico comprimido, calcula evidencia SHA-256 y sube el dump, su checksum y metadatos JSON. Usa un endpoint compatible con AWS CLI y cifrado KMS, pero deja host, puerto, endpoint, bucket, clave KMS y credenciales explícitos:

```bash
ops/mariadb/backup.sh \
  --source-host "$MARIADB_BACKUP_SOURCE_HOST" \
  --source-port "$MARIADB_BACKUP_SOURCE_PORT" \
  --source-database "$MARIADB_BACKUP_SOURCE_DATABASE" \
  --destination-uri "s3://$BACKUP_BUCKET/tuhospedaje/mariadb" \
  --kms-key-id "$PLATFORM_KMS_KEY_ID" \
  --defaults-file "$MARIADB_BACKUP_DEFAULTS_FILE" \
  --s3-endpoint "$S3_COMPATIBLE_ENDPOINT"
```

El archivo defaults es configuración de cliente MariaDB modo `0600`, propiedad del usuario, entregada por el secret manager o runtime. No pongas sus credenciales en historial de shell, volcados de entorno, logs, archivos fuente ni argumentos. El script no acepta contraseñas como argumento. Usa un directorio temporal privado y quita el dump transitorio en texto plano al salir; el almacenamiento efímero y los controles de acceso del host siguen bajo responsabilidad operativa.

La URI de destino es un prefijo, no un default del proveedor. Conservar el sidecar `*.sha256` y metadatos `*.json` junto al dump cifrado. El sidecar tiene una única entrada SHA-256 seguida del basename del dump; los metadatos guardan hora de creación, identificadores de origen, checksum SHA-256 y URI del objeto. El restore exige las URIs exactas `<backup-uri>.sha256` y `<backup-uri>.json`, y valida ambos contra el archivo descargado.

El checksum y metadatos detectan corrupción o desajustes, pero no prueban autenticidad por sí mismos. Los permisos de object storage, versionado e inmutabilidad son controles operativos externos a estos scripts.

## Verificación mensual de restore aislado

`restore-verify.sh` descarga un backup, checksum y metadata nombrados, verifica SHA-256 y que los campos `object_uri`/`sha256` de metadata coincidan con el backup validado. Requiere host y puerto del target explícitos; luego crea una base descartable nueva, restaura, valida la consulta, confirma historial de migraciones y cuenta tablas restauradas. También consulta un `--health-url` explícito de una instancia de aplicación no productiva, sin imprimir URL ni body. Escribe un JSON con resultado general, duración en segundos, identidad del target y estado pass/fail de cada control.

```bash
ops/mariadb/restore-verify.sh \
  --backup-uri "s3://$BACKUP_BUCKET/tuhospedaje/mariadb/mariadb-example.sql.gz" \
  --checksum-uri "s3://$BACKUP_BUCKET/tuhospedaje/mariadb/mariadb-example.sql.gz.sha256" \
  --metadata-uri "s3://$BACKUP_BUCKET/tuhospedaje/mariadb/mariadb-example.sql.gz.json" \
  --target-host "$DISPOSABLE_RESTORE_HOST" \
  --target-port "$DISPOSABLE_RESTORE_PORT" \
  --target-database "tuhospedaje_restore_verify_20250101" \
  --target-volume "tuhospedaje-restore-verify-20250101" \
  --health-url "$DISPOSABLE_RESTORE_APP_HEALTH_URL" \
  --defaults-file "$MARIADB_RESTORE_DEFAULTS_FILE" \
  --evidence-file "./restore-evidence-20250101.json" \
  --confirm-disposable-target \
  --s3-endpoint "$S3_COMPATIBLE_ENDPOINT"
```

Los nombres de base y volumen deben identificar visiblemente un target descartable `restore`/`verify`/`test`/`sandbox`. El host y health URL también deben incluir uno de esos marcadores, salvo `localhost` o `127.0.0.1`. El host, base, volumen y health URL se validan de forma explícita y los identificadores parecidos a producción se rechazan sin distinguir mayúsculas/minúsculas (`prod`, `production`, `live`, `primary`). El script exige confirmación explícita, rechaza una base o archivo de evidencia existente y registra incluso fallos de guards posteriores a una ruta de evidencia válida. Nunca hace `drop` de una base o volumen y no infiere el target desde Compose, desarrollo local ni settings de producción.

Ejecutar solo sobre infraestructura aislada autorizada para el ejercicio. El health probe es evidencia operativa: la asociación entre esa URL y la aplicación conectada al target restaurado debe verificarse y registrarse fuera del script. Conservar el JSON, URI del backup, fecha de ejecución, operador, identidad del target y cualquier incidente/escalamiento en el registro mensual. Los scripts no fijan credenciales, buckets, hosts, volúmenes, endpoints ni políticas de retención de producción.
