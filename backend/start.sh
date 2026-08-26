#!/usr/bin/env bash
# Load shell-safe variables from .env, then start Spring Boot.
# Dotted Spring keys (for example spring.profiles.active) are skipped here;
# Spring Boot still reads the file via spring.config.import.

load_env() {
  local file="$1"
  local line key value
  while IFS= read -r line || [ -n "$line" ]; do
    case "$line" in
      ''|\#*) continue ;;
    esac
    key="${line%%=*}"
    value="${line#*=}"
    case "$key" in
      ''|[0-9]*|*[!A-Za-z0-9_]*) continue ;;
    esac
    if [ "${#value}" -ge 2 ]; then
      case "$value" in
        \"*\") value="${value#\"}"; value="${value%\"}" ;;
        \'*\') value="${value#\'}"; value="${value%\'}" ;;
      esac
    fi
    export "${key}=${value}"
  done < "$file"
}

if [ -f .env ]; then
  load_env .env
elif [ -f ../.env ]; then
  load_env ../.env
fi

./mvnw spring-boot:run
