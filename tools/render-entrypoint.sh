#!/bin/sh
set -eu

# Render service references contain host:port, not a complete HTTP URL.
if [ -z "${INVENTORY_SERVICE_URL:-}" ] && [ -n "${INVENTORY_SERVICE_HOSTPORT:-}" ]; then
  export INVENTORY_SERVICE_URL="http://${INVENTORY_SERVICE_HOSTPORT}"
fi
if [ -z "${ORDER_SERVICE_URL:-}" ] && [ -n "${ORDER_SERVICE_HOSTPORT:-}" ]; then
  export ORDER_SERVICE_URL="http://${ORDER_SERVICE_HOSTPORT}"
fi
if [ -z "${PAYMENT_SERVICE_URL:-}" ] && [ -n "${PAYMENT_SERVICE_HOSTPORT:-}" ]; then
  export PAYMENT_SERVICE_URL="http://${PAYMENT_SERVICE_HOSTPORT}"
fi

# Keep credentials separate: a Render postgres:// URL is not a JDBC URL.
if [ -z "${SPRING_DATASOURCE_URL:-}" ] && [ -n "${PGHOST:-}" ]; then
  export SPRING_DATASOURCE_URL="jdbc:postgresql://${PGHOST}:${PGPORT:?PGPORT is required}/${PGDATABASE:?PGDATABASE is required}?sslmode=require"
fi

# exec preserves SIGTERM delivery and graceful shutdown of the Java process.
exec java -jar /app/app.jar "$@"
