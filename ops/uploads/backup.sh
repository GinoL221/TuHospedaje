#!/usr/bin/env bash
# Creates and uploads one encrypted archive of the uploaded media volume.
set -euo pipefail
IFS=$'\n\t'
umask 077

usage() {
  printf '%s\n' 'Usage: backup.sh --source-dir DIR --destination-uri s3://BUCKET/PREFIX --kms-key-id KEY [--s3-endpoint URL]'
}
die() {
  printf 'backup failed: %s\n' "$1" >&2
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

SOURCE_DIR=
DESTINATION_URI=
KMS_KEY_ID=
S3_ENDPOINT=
while (($#)); do
  case "$1" in
  --source-dir)
    SOURCE_DIR=${2-}
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

[[ -n $SOURCE_DIR && -n $DESTINATION_URI && -n $KMS_KEY_ID ]] || {
  usage >&2
  die 'all source directory, destination URI, and KMS key identifier inputs are required'
}
[[ -d $SOURCE_DIR ]] || die 'source directory must exist and be a directory'
[[ $DESTINATION_URI =~ ^s3://[A-Za-z0-9._-]+(/[A-Za-z0-9._/-]+)?$ ]] || die 'destination must be a safe s3:// bucket/prefix URI'
[[ $KMS_KEY_ID =~ ^[A-Za-z0-9:/_.-]+$ ]] || die 'KMS key identifier contains unsafe characters'

command -v tar >/dev/null || die 'tar is required'
command -v gzip >/dev/null || die 'gzip is required'
command -v aws >/dev/null || die 'AWS CLI-compatible object-storage client is required'
command -v sha256sum >/dev/null || die 'sha256sum is required'

TMPDIR=$(mktemp -d) || die 'could not create private temporary directory'
ARCHIVE=$TMPDIR/uploads.tar.gz
CHECKSUM_FILE=$TMPDIR/uploads.tar.gz.sha256
METADATA_FILE=$TMPDIR/uploads.json
cleanup() {
  if [[ -f $ARCHIVE ]]; then shred -u -- "$ARCHIVE" 2>/dev/null || rm -f -- "$ARCHIVE"; fi
  rm -f -- "$CHECKSUM_FILE" "$METADATA_FILE"
  rmdir -- "$TMPDIR" 2>/dev/null || true
}
trap cleanup EXIT

TIMESTAMP=$(date -u +%Y%m%dT%H%M%SZ)
OBJECT_PREFIX=${DESTINATION_URI%/}
OBJECT_URI="$OBJECT_PREFIX/uploads-$TIMESTAMP.tar.gz"
CHECKSUM_URI="$OBJECT_URI.sha256"
METADATA_URI="$OBJECT_URI.json"
AWS_ARGS=()
[[ -n $S3_ENDPOINT ]] && AWS_ARGS+=(--endpoint-url "$S3_ENDPOINT")

tar -czf "$ARCHIVE" -C "$SOURCE_DIR" .
[[ -s $ARCHIVE ]] || die 'uploads archive is empty'
CHECKSUM=$(sha256sum -- "$ARCHIVE" | awk '{print $1}')
printf '%s  %s\n' "$CHECKSUM" "${OBJECT_URI##*/}" >"$CHECKSUM_FILE"
printf '{"created_at":"%s","source_dir":"%s","sha256":"%s","object_uri":"%s"}\n' \
  "$TIMESTAMP" "$(json_escape "$SOURCE_DIR")" "$CHECKSUM" "$OBJECT_URI" >"$METADATA_FILE"

aws ${AWS_ARGS[@]+"${AWS_ARGS[@]}"} s3 cp "$ARCHIVE" "$OBJECT_URI" --sse aws:kms --sse-kms-key-id "$KMS_KEY_ID" --only-show-errors
aws ${AWS_ARGS[@]+"${AWS_ARGS[@]}"} s3 cp "$CHECKSUM_FILE" "$CHECKSUM_URI" --sse aws:kms --sse-kms-key-id "$KMS_KEY_ID" --only-show-errors
aws ${AWS_ARGS[@]+"${AWS_ARGS[@]}"} s3 cp "$METADATA_FILE" "$METADATA_URI" --sse aws:kms --sse-kms-key-id "$KMS_KEY_ID" --only-show-errors
printf 'uploads backup completed; checksum evidence uploaded\n'
