#!/usr/bin/env bash
# Creates and uploads one encrypted MariaDB logical backup. Credentials stay in a client defaults file.
set -euo pipefail
IFS=$'\n\t'
umask 077

usage() {
  printf '%s\n' 'Usage: backup.sh --source-host HOST --source-port PORT --source-database DATABASE --destination-uri s3://BUCKET/PREFIX --kms-key-id KEY --defaults-file PATH [--s3-endpoint URL]'
}
die() {
  printf 'backup failed: %s\n' "$1" >&2
  exit 1
}

SOURCE_HOST=
SOURCE_PORT=
SOURCE_DATABASE=
DESTINATION_URI=
KMS_KEY_ID=
DEFAULTS_FILE=
S3_ENDPOINT=
while (($#)); do
  case "$1" in
  --source-host)
    SOURCE_HOST=${2-}
    shift 2
    ;;
  --source-port)
    SOURCE_PORT=${2-}
    shift 2
    ;;
  --source-database)
    SOURCE_DATABASE=${2-}
    shift 2
    ;;
  --destination-uri)
    DESTINATION_URI=${2-}
    shift 2
    ;;
  --kms-key-id)
    KMS_KEY_ID=${2-}
    shift 2
    ;;
  --defaults-file)
    DEFAULTS_FILE=${2-}
    shift 2
    ;;
  --s3-endpoint)
    S3_ENDPOINT=${2-}
    shift 2
    ;;
  -h | --help)
    usage
    exit 0
    ;;
  *)
    usage >&2
    die 'unknown or incomplete argument'
    ;;
  esac
done
[[ -n $SOURCE_HOST && -n $SOURCE_PORT && -n $SOURCE_DATABASE && -n $DESTINATION_URI && -n $KMS_KEY_ID && -n $DEFAULTS_FILE ]] || {
  usage >&2
  die 'all source, destination, KMS, and credential-file inputs are required'
}
[[ $SOURCE_HOST =~ ^[A-Za-z0-9.-]+$ && $SOURCE_DATABASE =~ ^[A-Za-z0-9_-]+$ ]] || die 'source host or database contains unsafe characters'
[[ $SOURCE_PORT =~ ^[0-9]{1,5}$ ]] || die 'source port must be an integer from 1 to 65535'
((10#$SOURCE_PORT >= 1 && 10#$SOURCE_PORT <= 65535)) || die 'source port must be an integer from 1 to 65535'
[[ $DESTINATION_URI =~ ^s3://[A-Za-z0-9._-]+(/[A-Za-z0-9._/-]+)?$ ]] || die 'destination must be a safe s3:// bucket/prefix URI'
[[ $KMS_KEY_ID =~ ^[A-Za-z0-9:/_.-]+$ ]] || die 'KMS key identifier contains unsafe characters'
[[ -f $DEFAULTS_FILE && -O $DEFAULTS_FILE ]] || die 'credential defaults file must exist and be owned by this user'
[[ $(stat -c '%a' -- "$DEFAULTS_FILE") =~ ^[0-6]00$ ]] || die 'credential defaults file must not be group- or world-accessible'
command -v mariadb-dump >/dev/null || die 'mariadb-dump is required'
command -v aws >/dev/null || die 'AWS CLI-compatible object-storage client is required'
command -v sha256sum >/dev/null || die 'sha256sum is required'

TMPDIR=$(mktemp -d) || die 'could not create private temporary directory'
DUMP=$TMPDIR/backup.sql.gz
CHECKSUM_FILE=$TMPDIR/backup.sql.gz.sha256
METADATA_FILE=$TMPDIR/backup.json
cleanup() {
  if [[ -f $DUMP ]]; then shred -u -- "$DUMP" 2>/dev/null || rm -f -- "$DUMP"; fi
  rm -f -- "$CHECKSUM_FILE" "$METADATA_FILE"
  rmdir -- "$TMPDIR" 2>/dev/null || true
}
trap cleanup EXIT

TIMESTAMP=$(date -u +%Y%m%dT%H%M%SZ)
OBJECT_PREFIX=${DESTINATION_URI%/}
OBJECT_URI="$OBJECT_PREFIX/mariadb-$SOURCE_DATABASE-$TIMESTAMP.sql.gz"
CHECKSUM_URI="$OBJECT_URI.sha256"
METADATA_URI="$OBJECT_URI.json"
AWS_ARGS=()
[[ -n $S3_ENDPOINT ]] && AWS_ARGS+=(--endpoint-url "$S3_ENDPOINT")

mariadb-dump --defaults-extra-file="$DEFAULTS_FILE" --host="$SOURCE_HOST" --port="$SOURCE_PORT" \
  --single-transaction --routines --events --triggers --skip-comments "$SOURCE_DATABASE" | gzip -9 >"$DUMP"
[[ -s $DUMP ]] || die 'database dump is empty'
CHECKSUM=$(sha256sum -- "$DUMP" | awk '{print $1}')
printf '%s  %s\n' "$CHECKSUM" "${OBJECT_URI##*/}" >"$CHECKSUM_FILE"
printf '{"created_at":"%s","source_host":"%s","source_database":"%s","sha256":"%s","object_uri":"%s"}\n' \
  "$TIMESTAMP" "$SOURCE_HOST" "$SOURCE_DATABASE" "$CHECKSUM" "$OBJECT_URI" >"$METADATA_FILE"

aws "${AWS_ARGS[@]}" s3 cp "$DUMP" "$OBJECT_URI" --sse aws:kms --sse-kms-key-id "$KMS_KEY_ID" --only-show-errors
aws "${AWS_ARGS[@]}" s3 cp "$CHECKSUM_FILE" "$CHECKSUM_URI" --sse aws:kms --sse-kms-key-id "$KMS_KEY_ID" --only-show-errors
aws "${AWS_ARGS[@]}" s3 cp "$METADATA_FILE" "$METADATA_URI" --sse aws:kms --sse-kms-key-id "$KMS_KEY_ID" --only-show-errors
printf 'backup completed; checksum evidence uploaded\n'
