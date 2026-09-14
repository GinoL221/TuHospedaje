#!/usr/bin/env bash
set -Eeuo pipefail

require_value() {
  local name=$1
  if [[ -z ${!name:-} ]]; then
    printf '%s is required\n' "$name" >&2
    exit 1
  fi
}

validate_identifier() {
  local name=$1
  local value=${!name}
  if [[ ! $value =~ ^[A-Za-z_][A-Za-z0-9_]{0,63}$ ]]; then
    printf '%s must be a valid MariaDB identifier\n' "$name" >&2
    exit 1
  fi
}

for required in \
  PROD_DB_NAME \
  PROD_DB_ROOT_PASSWORD \
  DB_RUNTIME_USERNAME \
  DB_RUNTIME_PASSWORD \
  DB_MIGRATION_USERNAME \
  DB_MIGRATION_PASSWORD; do
  require_value "$required"
done

for identifier in PROD_DB_NAME DB_RUNTIME_USERNAME DB_MIGRATION_USERNAME; do
  validate_identifier "$identifier"
done

if [[ $DB_RUNTIME_USERNAME == "$DB_MIGRATION_USERNAME" ]]; then
  printf 'DB_RUNTIME_USERNAME and DB_MIGRATION_USERNAME must differ\n' >&2
  exit 1
fi

runtime_password_b64=$(printf %s "$DB_RUNTIME_PASSWORD" | base64 | tr -d '\n')
migration_password_b64=$(printf %s "$DB_MIGRATION_PASSWORD" | base64 | tr -d '\n')

export MYSQL_PWD="$PROD_DB_ROOT_PASSWORD"

mariadb --host=db --port=3306 --user=root --protocol=tcp <<SQL
SET @database_name = '${PROD_DB_NAME}';
SET @runtime_user = '${DB_RUNTIME_USERNAME}';
SET @migration_user = '${DB_MIGRATION_USERNAME}';
SET @runtime_password = FROM_BASE64('${runtime_password_b64}');
SET @migration_password = FROM_BASE64('${migration_password_b64}');

SET @statement = CONCAT(
  'CREATE USER IF NOT EXISTS ''', @runtime_user, '''@''%'' IDENTIFIED BY ', QUOTE(@runtime_password)
);
PREPARE statement FROM @statement;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @statement = CONCAT(
  'ALTER USER ''', @runtime_user, '''@''%'' IDENTIFIED BY ', QUOTE(@runtime_password)
);
PREPARE statement FROM @statement;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @statement = CONCAT(
  'CREATE USER IF NOT EXISTS ''', @migration_user, '''@''%'' IDENTIFIED BY ', QUOTE(@migration_password)
);
PREPARE statement FROM @statement;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @statement = CONCAT(
  'ALTER USER ''', @migration_user, '''@''%'' IDENTIFIED BY ', QUOTE(@migration_password)
);
PREPARE statement FROM @statement;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @statement = CONCAT('REVOKE ALL PRIVILEGES, GRANT OPTION FROM ''', @runtime_user, '''@''%''');
PREPARE statement FROM @statement;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @statement = CONCAT('REVOKE ALL PRIVILEGES, GRANT OPTION FROM ''', @migration_user, '''@''%''');
PREPARE statement FROM @statement;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @statement = CONCAT(
  'GRANT SELECT, INSERT, UPDATE, DELETE ON ', @database_name, '.* TO ''', @runtime_user, '''@''%'''
);
PREPARE statement FROM @statement;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @statement = CONCAT(
  'GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, DROP, INDEX, REFERENCES ON ', @database_name, '.* TO ''', @migration_user, '''@''%'''
);
PREPARE statement FROM @statement;
EXECUTE statement;
DEALLOCATE PREPARE statement;
SQL

unset MYSQL_PWD
printf 'Provisioned runtime account %s and migration account %s for database %s\n' \
  "$DB_RUNTIME_USERNAME" "$DB_MIGRATION_USERNAME" "$PROD_DB_NAME"
