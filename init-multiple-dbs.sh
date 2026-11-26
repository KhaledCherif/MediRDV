#!/bin/bash
# Ensure all service roles and databases exist, including on an existing volume.

set -euo pipefail

psql_base=(psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB")

ensure_role() {
    local role="$1"
    local password="$2"

    if "${psql_base[@]}" -tAc "SELECT 1 FROM pg_roles WHERE rolname = '$role'" | grep -q 1; then
        "${psql_base[@]}" -c "ALTER USER \"$role\" WITH PASSWORD '$password'"
    else
        "${psql_base[@]}" -c "CREATE USER \"$role\" WITH PASSWORD '$password'"
    fi
}

ensure_database() {
    local database="$1"
    local owner="$2"

    if ! "${psql_base[@]}" -tAc "SELECT 1 FROM pg_database WHERE datname = '$database'" | grep -q 1; then
        "${psql_base[@]}" -c "CREATE DATABASE \"$database\" OWNER \"$owner\""
    fi
}

ensure_role "praticien_user" "praticien_pass"
ensure_database "praticien_db" "praticien_user"

ensure_role "rdv_user" "rdv_pass"
ensure_database "rdv_db" "rdv_user"

ensure_role "assurance" "assurance123"
ensure_database "assurance_legacy" "assurance"

echo "Service databases and roles are ready"
