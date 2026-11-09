# Backup y recuperación del volumen de uploads

Este runbook define los controles de respaldo y recuperación para el volumen de archivos multimedia subidos por administradores (`tuhospedaje-prod-uploads` en `compose.prod.yaml`). Complementa el [runbook de MariaDB](../mariadb/README.md).

## Política y responsables

- **RPO:** 24 horas. Operaciones programa el respaldo diario de uploads en la misma ventana del backup de base de datos para mantener consistencia referencial.
- **RTO:** 4 horas. Los responsables de plataforma mantienen procedimiento y accesos para restaurar el volumen dentro de la misma ventana de recuperación de la base.
- **Retención:** conservar los archivos `.tar.gz` diarios y sus archivos sidecar `.sha256` y `.json` durante 30 días en object storage compatible con S3.
- **Almacenamiento:** object storage cifrado del lado del servidor con KMS (`--sse aws:kms --sse-kms-key-id`).
- **Consistencia:** cada imagen referenciada en las columnas `image_url` de `lodging_images` o `categories` con prefijo `/uploads/` debe existir en el volumen restaurado.

## Respaldo del volumen

`backup.sh` empaqueta el contenido de uploads, calcula evidencia SHA-256 y sube el archivo tarball comprimido, su checksum y metadatos JSON a S3:

```bash
ops/uploads/backup.sh \
  --source-dir "$UPLOADS_SOURCE_DIR" \
  --destination-uri "s3://$BACKUP_BUCKET/tuhospedaje/uploads" \
  --kms-key-id "$PLATFORM_KMS_KEY_ID" \
  --s3-endpoint "$S3_COMPATIBLE_ENDPOINT"
```

### Ejecución en contenedor con el volumen de producción

Para respaldar el volumen Docker `tuhospedaje-prod-uploads` en un contenedor (o runner de CI/CD) que disponga de `aws-cli`, `tar` y `gzip`:

```bash
docker run --rm \
  -v tuhospedaje-prod-uploads:/data/uploads:ro \
  -v "$PWD/ops/uploads/backup.sh":/usr/local/bin/backup-uploads.sh:ro \
  -e AWS_ACCESS_KEY_ID \
  -e AWS_SECRET_ACCESS_KEY \
  -e AWS_DEFAULT_REGION \
  my-platform/backup-runner:latest /usr/local/bin/backup-uploads.sh \
    --source-dir /data/uploads \
    --destination-uri "s3://$BACKUP_BUCKET/tuhospedaje/uploads" \
    --kms-key-id "$PLATFORM_KMS_KEY_ID"
```

El montaje `:ro` protege el volumen de modificaciones accidentales durante la lectura del respaldo.

## Procedimiento de restauración

- **Descarga y verificación:** descargar el archivo `.tar.gz` y verificar su integridad contra el archivo `.sha256` correspondiente:

```bash
sha256sum -c "uploads-$TIMESTAMP.tar.gz.sha256"
```

- **Extracción:** descomprimir el contenido en el volumen o directorio de destino:

```bash
tar -xzf "uploads-$TIMESTAMP.tar.gz" -C /ruta/al/volumen/uploads
```

- **Verificación de consistencia:** cruzar la base de datos restaurada con los archivos existentes en disco:

```bash
# Comprobar que todo archivo referenciado en lodging_images o categories exista en el volumen
mariadb --defaults-extra-file="$MARIADB_RESTORE_DEFAULTS_FILE" \
  -h "$DISPOSABLE_RESTORE_HOST" -P "$DISPOSABLE_RESTORE_PORT" "$TARGET_DATABASE" \
  -e "SELECT image_url FROM lodging_images WHERE image_url LIKE '/uploads/%' UNION SELECT image_url FROM categories WHERE image_url LIKE '/uploads/%';" -N -B | \
while read -r img; do
  file_path="/ruta/al/volumen${img}"
  if [[ ! -f "$file_path" ]]; then
    echo "ALERTA: Imagen faltante en volumen restaurado: $file_path"
  fi
done
```
