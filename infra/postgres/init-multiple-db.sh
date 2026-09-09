#!/bin/bash
# Creates one PostgreSQL database per microservice (database-per-service,
# requirement doc section 2.4) the first time the postgres container starts.
set -e

for DB in account_db transaction_db notification_db; do
  echo "Creating database: $DB"
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    SELECT 'CREATE DATABASE $DB' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '$DB')\gexec
EOSQL
done
