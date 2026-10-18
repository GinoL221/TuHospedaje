#!/bin/sh
set -eu

if ! hash=$(awk -F= '/^DEV_ADMIN_PASSWORD_HASH=/ { print substr($0, index($0, "=") + 1); found = 1; exit } END { if (!found) exit 1 }' /run/dev-admin.env); then
  echo "dev admin hash is missing or malformed" >&2
  exit 1
fi
prefix=$(printf %s "$hash" | cut -c1-4)
# Bcrypt prefixes contain literal dollar signs, not shell variables.
# shellcheck disable=SC2016
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
# Validate the encoded format only; never compute a hash or print its value.
# Dollar signs belong to the grep expression, not shell expansion.
# shellcheck disable=SC2016
if ! printf %s "$hash" | LC_ALL=C grep -Eq '^\$2[aby]\$(0[4-9]|[12][0-9]|3[01])\$[./A-Za-z0-9]{53}$'; then
  echo "dev admin hash is missing or malformed" >&2
  exit 1
fi
export DEV_ADMIN_PASSWORD_HASH="$hash"
exec /usr/local/bin/dev-entrypoint.sh
