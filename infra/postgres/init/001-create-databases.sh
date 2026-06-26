#!/usr/bin/env bash
set -euo pipefail

create_database_if_missing() {
  local db_name="$1"

  if psql \
    --username "$POSTGRES_USER" \
    --dbname "$POSTGRES_DB" \
    --tuples-only \
    --command "SELECT 1 FROM pg_database WHERE datname = '$db_name';" | grep -q 1; then
    echo "Database '$db_name' already exists"
  else
    echo "Creating database '$db_name'"
    createdb --username "$POSTGRES_USER" "$db_name"
  fi
}

create_database_if_missing "$AUTH_SERVICE_DB_NAME"
create_database_if_missing "$VOTING_SERVICE_DB_NAME"