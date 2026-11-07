#!/usr/bin/env bash
# ============================================================================
# V-SAT Compass: Run & Verify Database Migrations (PostgreSQL 17 + Flyway)
# ============================================================================
set -euo pipefail

ENV_FILE="${ENV_FILE:-.env}"
if [[ -f "$ENV_FILE" ]]; then
  # shellcheck disable=SC1090
  source "$ENV_FILE"
fi

CONTAINER_NAME="${PG_CONTAINER:-vsat-postgres}"
DB_NAME="${POSTGRES_DB:-vsat_compass}"
DB_USER="${POSTGRES_USER:-vsat_prod_user}"
MAX_WAIT_SECONDS="${MIGRATION_WAIT_SECONDS:-60}"

echo "============================================================"
echo "[OPS] Checking PostgreSQL Connection & Flyway Migrations"
echo "  Container : ${CONTAINER_NAME}"
echo "  Database  : ${DB_NAME}"
echo "  User      : ${DB_USER}"
echo "============================================================"

# 1. Check PostgreSQL readiness probe
echo "[OPS] 1/3 Waiting for PostgreSQL readiness..."
READY=0
for ((i=1; i<=MAX_WAIT_SECONDS; i++)); do
  if docker exec "$CONTAINER_NAME" pg_isready -U "$DB_USER" -d "$DB_NAME" >/dev/null 2>&1; then
    READY=1
    echo "[OPS] PostgreSQL is ready (attempt ${i}s)."
    break
  fi
  sleep 1
done

if [[ "$READY" -ne 1 ]]; then
  echo "[OPS ERROR] PostgreSQL container '${CONTAINER_NAME}' did not become ready within ${MAX_WAIT_SECONDS}s."
  exit 1
fi

# 2. Check if Flyway schema history table exists
echo "[OPS] 2/3 Checking flyway_schema_history table..."
TABLE_EXISTS=$(docker exec -i "$CONTAINER_NAME" psql -U "$DB_USER" -d "$DB_NAME" -tAc \
  "SELECT to_regclass('public.flyway_schema_history');" 2>/dev/null || echo "")

if [[ -z "$TABLE_EXISTS" || "$TABLE_EXISTS" == "" ]]; then
  echo "[OPS WARN] Table 'flyway_schema_history' not found yet."
  echo "[OPS INFO] If Spring Boot is still booting, Flyway will initialize the table automatically."
  echo "[OPS INFO] Checking base tables in database..."
  BASE_TABLES=$(docker exec -i "$CONTAINER_NAME" psql -U "$DB_USER" -d "$DB_NAME" -tAc \
    "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public';")
  echo "[OPS INFO] Found ${BASE_TABLES} public tables."
  exit 0
fi

# 3. Query applied migration history
echo "[OPS] 3/3 Verifying applied migration versions..."
MIGRATION_COUNT=$(docker exec -i "$CONTAINER_NAME" psql -U "$DB_USER" -d "$DB_NAME" -tAc \
  "SELECT count(*) FROM flyway_schema_history WHERE success = true;")

FAILED_COUNT=$(docker exec -i "$CONTAINER_NAME" psql -U "$DB_USER" -d "$DB_NAME" -tAc \
  "SELECT count(*) FROM flyway_schema_history WHERE success = false;")

LATEST_VERSION=$(docker exec -i "$CONTAINER_NAME" psql -U "$DB_USER" -d "$DB_NAME" -tAc \
  "SELECT version || ' - ' || description FROM flyway_schema_history WHERE success = true ORDER BY installed_rank DESC LIMIT 1;")

echo "------------------------------------------------------------"
echo "  Total successful migrations : ${MIGRATION_COUNT}"
echo "  Failed migrations           : ${FAILED_COUNT}"
echo "  Latest applied migration    : ${LATEST_VERSION}"
echo "------------------------------------------------------------"

if [[ "$FAILED_COUNT" -gt 0 ]]; then
  echo "[OPS ERROR] One or more Flyway migrations failed! Check flyway_schema_history table."
  docker exec -i "$CONTAINER_NAME" psql -U "$DB_USER" -d "$DB_NAME" -c \
    "SELECT installed_rank, version, description, success, installed_on FROM flyway_schema_history WHERE success = false;"
  exit 1
fi

echo "[OPS SUCCESS] Database migrations verified cleanly."
exit 0
