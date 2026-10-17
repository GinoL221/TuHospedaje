#!/bin/sh
set -eu

hash=$(awk -F= '/^DEV_ADMIN_PASSWORD_HASH=/ { print substr($0, index($0, "=") + 1); found = 1; exit } END { if (!found) exit 1 }' /run/dev-admin.env)
prefix=$(printf %s "$hash" | cut -c1-4)
case "$prefix" in
  '$2a$'|'$2b$'|'$2y$') ;;
  *)
    echo "dev admin hash is missing or malformed" >&2
    exit 1
    ;;
esac
if [ "${#hash}" -ne 60 ]; then
  echo "dev admin hash has the wrong length" >&2
  exit 1
fi
export DEV_ADMIN_PASSWORD_HASH="$hash"
exec /usr/local/bin/dev-entrypoint.sh
