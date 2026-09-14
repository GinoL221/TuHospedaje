#!/bin/sh
set -eu

child_pid=""

start_app() {
  JAVA_TOOL_OPTIONS="-Dspring.devtools.restart.enabled=false" ./mvnw spring-boot:run &
  child_pid=$!
}

stop_app() {
  if [ -n "$child_pid" ] && kill -0 "$child_pid" 2>/dev/null; then
    kill -TERM "$child_pid"
    wait "$child_pid" || true
  fi
  child_pid=""
}

shutdown() {
  stop_app
  exit 0
}

trap shutdown TERM INT

start_app
while kill -0 "$child_pid" 2>/dev/null; do
  if inotifywait -q -r -e modify,close_write,move,create,delete --timeout 2 src/main pom.xml; then
    stop_app
    start_app
  fi
done

wait "$child_pid"
