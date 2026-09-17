#!/usr/bin/env bash
# Restores a backup only into an explicit, disposable MariaDB target and records the result.
set -euo pipefail
IFS=$'\n\t'
umask 077

usage() {
  printf '%s\n' 'Usage: restore-verify.sh --backup-uri s3://...sql.gz --checksum-uri s3://...sha256 --metadata-uri s3://...sql.gz.json --target-host HOST --target-port PORT --target-database DATABASE --target-volume NAME --health-url URL --defaults-file PATH --evidence-file PATH --confirm-disposable-target [--s3-endpoint URL]'
}
die() {
  printf 'restore verification failed: %s\n' "$1" >&2
  exit 1
}
json_escape() {
  local value=$1
  value=${value//\\/\\\\}
  value=${value//\"/\\\"}
  value=${value//$'\n'/\\n}
  value=${value//$'\r'/\\r}
  value=${value//$'\t'/\\t}
  printf '%s' "$value"
}

BACKUP_URI= CHECKSUM_URI= METADATA_URI= TARGET_HOST= TARGET_PORT= TARGET_DATABASE= TARGET_VOLUME= HEALTH_URL= DEFAULTS_FILE= EVIDENCE_FILE= S3_ENDPOINT= CONFIRMED=false
CHECK_DOWNLOAD=not_run CHECKSUM_VALID=not_run CHECK_METADATA=not_run TARGET_ABSENT=not_run DATABASE_CREATED=not_run RESTORE_COMPLETED=not_run DATABASE_QUERY=not_run CHECK_MIGRATION_HISTORY=not_run CHECK_TABLE_COUNT=not_run CHECK_HEALTH=not_run
START_EPOCH=$(date +%s)
record_evidence() {
  local exit_code=$1 outcome=fail end_epoch duration
  ((exit_code == 0)) && outcome=pass
  end_epoch=$(date +%s)
  duration=$((end_epoch - START_EPOCH))
  printf '{"outcome":"%s","duration_seconds":%s,"target":{"host":"%s","port":"%s","database":"%s","volume":"%s"},"checks":{"download":"%s","checksum":"%s","metadata":"%s","target_absent":"%s","database_created":"%s","restore":"%s","database_query":"%s","migration_history":"%s","table_count":"%s","application_health":"%s"}}\n' \
    "$outcome" "$duration" "$(json_escape "$TARGET_HOST")" "$(json_escape "$TARGET_PORT")" "$(json_escape "$TARGET_DATABASE")" "$(json_escape "$TARGET_VOLUME")" \
    "$CHECK_DOWNLOAD" "$CHECKSUM_VALID" "$CHECK_METADATA" "$TARGET_ABSENT" "$DATABASE_CREATED" "$RESTORE_COMPLETED" "$DATABASE_QUERY" "$CHECK_MIGRATION_HISTORY" "$CHECK_TABLE_COUNT" "$CHECK_HEALTH" >"$EVIDENCE_FILE"
}
on_exit() {
  local exit_code=$?
  record_evidence "$exit_code"
  trap - EXIT
  exit "$exit_code"
}

while (($#)); do
  case "$1" in
  --backup-uri)
    BACKUP_URI=${2-}
    shift 2
    ;;
  --checksum-uri)
    CHECKSUM_URI=${2-}
    shift 2
    ;;
  --metadata-uri)
    METADATA_URI=${2-}
    shift 2
    ;;
  --target-host)
    TARGET_HOST=${2-}
    shift 2
    ;;
  --target-port)
    TARGET_PORT=${2-}
    shift 2
    ;;
  --target-database)
    TARGET_DATABASE=${2-}
    shift 2
    ;;
  --target-volume)
    TARGET_VOLUME=${2-}
    shift 2
    ;;
  --health-url)
    HEALTH_URL=${2-}
    shift 2
    ;;
  --defaults-file)
    DEFAULTS_FILE=${2-}
    shift 2
    ;;
  --evidence-file)
    EVIDENCE_FILE=${2-}
    shift 2
    ;;
  --s3-endpoint)
    S3_ENDPOINT=${2-}
    shift 2
    ;;
  --confirm-disposable-target)
    CONFIRMED=true
    shift
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
[[ -n $EVIDENCE_FILE ]] || {
  usage >&2
  die 'a new evidence file is required'
}
[[ ! -e $EVIDENCE_FILE && -d $(dirname -- "$EVIDENCE_FILE") ]] || die 'evidence file must be new and its parent directory must exist'
trap on_exit EXIT

[[ -n $BACKUP_URI && -n $CHECKSUM_URI && -n $METADATA_URI && -n $TARGET_HOST && -n $TARGET_PORT && -n $TARGET_DATABASE && -n $TARGET_VOLUME && -n $HEALTH_URL && -n $DEFAULTS_FILE ]] || {
  usage >&2
  die 'all backup, target, health, and credential-file inputs are required'
}
[[ $CONFIRMED == true ]] || die 'explicit --confirm-disposable-target acknowledgement is required'
[[ $BACKUP_URI =~ ^s3://[A-Za-z0-9._/-]+\.sql\.gz$ && $CHECKSUM_URI == "$BACKUP_URI.sha256" && $METADATA_URI == "$BACKUP_URI.json" ]] || die 'backup, checksum, and metadata URIs must match safe S3 objects'
[[ $TARGET_HOST =~ ^[A-Za-z0-9.-]+$ && $TARGET_DATABASE =~ ^[A-Za-z0-9_-]+$ && $TARGET_VOLUME =~ ^[A-Za-z0-9._-]+$ ]] || die 'target identifiers contain unsafe characters'
[[ $TARGET_PORT =~ ^[0-9]{1,5}$ ]] || die 'target port must be an integer from 1 to 65535'
((10#$TARGET_PORT >= 1 && 10#$TARGET_PORT <= 65535)) || die 'target port must be an integer from 1 to 65535'
[[ $HEALTH_URL =~ ^https?://[A-Za-z0-9.-]+(:[0-9]{1,5})?(/[A-Za-z0-9._~:/?&=+,%@-]*)?$ ]] || die 'health URL must be a safe HTTP(S) URL'
TARGET_HOST_LOWER=${TARGET_HOST,,}
TARGET_DATABASE_LOWER=${TARGET_DATABASE,,}
TARGET_VOLUME_LOWER=${TARGET_VOLUME,,}
HEALTH_URL_LOWER=${HEALTH_URL,,}
[[ ! $TARGET_HOST_LOWER =~ (prod|production|live|primary) && ! $TARGET_DATABASE_LOWER =~ (prod|production|live|primary) && ! $TARGET_VOLUME_LOWER =~ (prod|production|live|primary) && ! $HEALTH_URL_LOWER =~ (prod|production|live|primary) ]] || die 'production-like targets and health URLs are forbidden'
[[ $TARGET_HOST_LOWER == localhost || $TARGET_HOST_LOWER == 127.0.0.1 || $TARGET_HOST_LOWER =~ (restore|verify|test|sandbox|disposable) ]] || die 'target host must have a disposable marker or be local'
[[ $HEALTH_URL_LOWER =~ ^https?://(localhost|127\.0\.0\.1)(:|/|$) || $HEALTH_URL_LOWER =~ (restore|verify|test|sandbox|disposable) ]] || die 'health URL must have a disposable marker or be local'
[[ $TARGET_DATABASE_LOWER =~ (restore|verify|test|sandbox|disposable) && $TARGET_VOLUME_LOWER =~ (restore|verify|test|sandbox|disposable) ]] || die 'database and volume must visibly identify a disposable target'
[[ -f $DEFAULTS_FILE && -O $DEFAULTS_FILE ]] || die 'credential defaults file must exist and be owned by this user'
[[ $(stat -c '%a' -- "$DEFAULTS_FILE") =~ ^[0-6]00$ ]] || die 'credential defaults file must not be group- or world-accessible'
command -v aws >/dev/null || die 'AWS CLI-compatible object-storage client is required'
command -v mariadb >/dev/null || die 'mariadb client is required'
command -v gzip >/dev/null || die 'gzip is required'
command -v sha256sum >/dev/null || die 'sha256sum is required'
command -v curl >/dev/null || die 'curl is required'

TMPDIR=$(mktemp -d) || die 'could not create private temporary directory'
BACKUP_FILE=$TMPDIR/${BACKUP_URI##*/}
CHECKSUM_FILE=$TMPDIR/${CHECKSUM_URI##*/}
METADATA_FILE=$TMPDIR/${METADATA_URI##*/}
cleanup() {
  if [[ -f $BACKUP_FILE ]]; then shred -u -- "$BACKUP_FILE" 2>/dev/null || rm -f -- "$BACKUP_FILE"; fi
  rm -f -- "$CHECKSUM_FILE" "$METADATA_FILE"
  rmdir -- "$TMPDIR" 2>/dev/null || true
}
on_exit_with_cleanup() {
  local exit_code=$?
  cleanup
  record_evidence "$exit_code"
  trap - EXIT
  exit "$exit_code"
}
trap on_exit_with_cleanup EXIT
AWS_ARGS=()
[[ -n $S3_ENDPOINT ]] && AWS_ARGS+=(--endpoint-url "$S3_ENDPOINT")

CHECK_DOWNLOAD=fail
CHECK_METADATA=fail
aws "${AWS_ARGS[@]}" s3 cp "$BACKUP_URI" "$BACKUP_FILE" --only-show-errors && aws "${AWS_ARGS[@]}" s3 cp "$CHECKSUM_URI" "$CHECKSUM_FILE" --only-show-errors && aws "${AWS_ARGS[@]}" s3 cp "$METADATA_URI" "$METADATA_FILE" --only-show-errors && CHECK_DOWNLOAD=pass
[[ $CHECK_DOWNLOAD == pass ]] || die 'could not download backup evidence'
CHECKSUM_VALID=fail
EXPECTED_BASENAME=${BACKUP_URI##*/}
[[ $(wc -l <"$CHECKSUM_FILE") -eq 1 ]] || die 'checksum sidecar must contain exactly one entry'
SIDECAR_LINE=$(<"$CHECKSUM_FILE")
[[ $SIDECAR_LINE =~ ^([A-Fa-f0-9]{64})\ \ (.+)$ ]] || die 'checksum sidecar has an invalid format'
EXPECTED_SHA256=${BASH_REMATCH[1]}
SIDECAR_BASENAME=${BASH_REMATCH[2]}
[[ $SIDECAR_BASENAME == "$EXPECTED_BASENAME" ]] || die 'checksum sidecar does not identify the downloaded backup'
ACTUAL_SHA256=$(sha256sum -- "$BACKUP_FILE" | awk '{print $1}')
[[ $ACTUAL_SHA256 == "$EXPECTED_SHA256" ]] && CHECKSUM_VALID=pass
[[ $CHECKSUM_VALID == pass ]] || die 'backup checksum does not match'
CHECK_METADATA=fail
[[ $(wc -l <"$METADATA_FILE") -eq 1 ]] || die 'metadata sidecar must contain exactly one entry'
METADATA_LINE=$(<"$METADATA_FILE")
[[ $METADATA_LINE =~ ^\{\"created_at\":\"[0-9]{8}T[0-9]{6}Z\",\"source_host\":\"[A-Za-z0-9.-]+\",\"source_database\":\"[A-Za-z0-9_-]+\",\"sha256\":\"([A-Fa-f0-9]{64})\",\"object_uri\":\"(s3://[A-Za-z0-9._/-]+\.sql\.gz)\"\}$ ]] || die 'metadata sidecar has an invalid format'
METADATA_SHA256=${BASH_REMATCH[1]}
METADATA_OBJECT_URI=${BASH_REMATCH[2]}
[[ $METADATA_SHA256 == "$EXPECTED_SHA256" && $METADATA_OBJECT_URI == "$BACKUP_URI" ]] && CHECK_METADATA=pass
[[ $CHECK_METADATA == pass ]] || die 'metadata does not match the validated backup'
TARGET_ABSENT=fail
[[ -z $(mariadb --defaults-extra-file="$DEFAULTS_FILE" --host="$TARGET_HOST" --port="$TARGET_PORT" --batch --skip-column-names --execute "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME='$TARGET_DATABASE'") ]] && TARGET_ABSENT=pass
[[ $TARGET_ABSENT == pass ]] || die 'target database already exists; it will not be overwritten'
DATABASE_CREATED=fail
mariadb --defaults-extra-file="$DEFAULTS_FILE" --host="$TARGET_HOST" --port="$TARGET_PORT" --execute "CREATE DATABASE \`$TARGET_DATABASE\`" && DATABASE_CREATED=pass
[[ $DATABASE_CREATED == pass ]] || die 'could not create disposable target database'
RESTORE_COMPLETED=fail
gzip -dc -- "$BACKUP_FILE" | mariadb --defaults-extra-file="$DEFAULTS_FILE" --host="$TARGET_HOST" --port="$TARGET_PORT" "$TARGET_DATABASE" && RESTORE_COMPLETED=pass
[[ $RESTORE_COMPLETED == pass ]] || die 'restore command failed'
DATABASE_QUERY=fail
[[ $(mariadb --defaults-extra-file="$DEFAULTS_FILE" --host="$TARGET_HOST" --port="$TARGET_PORT" "$TARGET_DATABASE" --batch --skip-column-names --execute 'SELECT 1') == 1 ]] && DATABASE_QUERY=pass
[[ $DATABASE_QUERY == pass ]] || die 'restored database query failed'
CHECK_MIGRATION_HISTORY=fail
MIGRATION_COUNT=$(mariadb --defaults-extra-file="$DEFAULTS_FILE" --host="$TARGET_HOST" --port="$TARGET_PORT" "$TARGET_DATABASE" --batch --skip-column-names --execute 'SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1')
[[ $MIGRATION_COUNT =~ ^[1-9][0-9]*$ ]] && CHECK_MIGRATION_HISTORY=pass
[[ $CHECK_MIGRATION_HISTORY == pass ]] || die 'restored migration history is missing or invalid'
CHECK_TABLE_COUNT=fail
TABLE_COUNT=$(mariadb --defaults-extra-file="$DEFAULTS_FILE" --host="$TARGET_HOST" --port="$TARGET_PORT" "$TARGET_DATABASE" --batch --skip-column-names --execute "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = '$TARGET_DATABASE'")
[[ $TABLE_COUNT =~ ^[1-9][0-9]*$ ]] && CHECK_TABLE_COUNT=pass
[[ $CHECK_TABLE_COUNT == pass ]] || die 'restored table count is empty or invalid'
CHECK_HEALTH=fail
curl --fail --silent --output /dev/null --max-time 30 "$HEALTH_URL" && CHECK_HEALTH=pass
[[ $CHECK_HEALTH == pass ]] || die 'isolated application health probe failed'
printf 'restore verification passed; evidence record written\n'
