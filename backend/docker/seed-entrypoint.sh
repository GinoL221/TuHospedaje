#!/bin/sh
set -eu

# Read one bcrypt hash from the mounted env file without Compose interpolation,
# validate its encoded format only, and print it. Never compute or log a hash.
# $1: variable name in the env file; $2: label used in error messages.
read_hash() {
  if ! value=$(awk -v key="$1" -F= 'index($0, key "=") == 1 { print substr($0, index($0, "=") + 1); found = 1; exit } END { if (!found) exit 1 }' /run/dev-admin.env); then
    echo "dev $2 hash is missing or malformed" >&2
    return 1
  fi
  # One pair of surrounding single quotes is optional: it keeps Compose from
  # interpolating the '$' signs when the same file is passed as --env-file.
  case "$value" in
    \'*\') value=${value#\'}; value=${value%\'} ;;
  esac
  prefix=$(printf %s "$value" | cut -c1-4)
  # Bcrypt prefixes contain literal dollar signs, not shell variables.
  # shellcheck disable=SC2016
  case "$prefix" in
    '$2a$'|'$2b$'|'$2y$') ;;
    *)
      echo "dev $2 hash is missing or malformed" >&2
      return 1
      ;;
  esac
  if [ "${#value}" -ne 60 ]; then
    echo "dev $2 hash has the wrong length" >&2
    return 1
  fi
  # Dollar signs belong to the grep expression, not shell expansion.
  # shellcheck disable=SC2016
  if ! printf %s "$value" | LC_ALL=C grep -Eq '^\$2[aby]\$(0[4-9]|[12][0-9]|3[01])\$[./A-Za-z0-9]{53}$'; then
    echo "dev $2 hash is missing or malformed" >&2
    return 1
  fi
  printf %s "$value"
}

admin_hash=$(read_hash DEV_ADMIN_PASSWORD_HASH admin) || exit 1
guest_hash=$(read_hash DEV_GUEST_PASSWORD_HASH guest) || exit 1
export DEV_ADMIN_PASSWORD_HASH="$admin_hash"
export DEV_GUEST_PASSWORD_HASH="$guest_hash"
exec /usr/local/bin/dev-entrypoint.sh
