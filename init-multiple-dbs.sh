#!/bin/bash
# Script to create multiple databases and users in PostgreSQL
# This runs during container initialization

set -e

# Create databases and users for each service
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    -- praticien-service database
    CREATE USER praticien_user WITH PASSWORD 'praticien_pass';
    CREATE DATABASE praticien_db OWNER praticien_user;
    GRANT ALL PRIVILEGES ON DATABASE praticien_db TO praticien_user;

    -- rendezvous-service database
    CREATE USER rdv_user WITH PASSWORD 'rdv_pass';
    CREATE DATABASE rdv_db OWNER rdv_user;
    GRANT ALL PRIVILEGES ON DATABASE rdv_db TO rdv_user;

    -- assurance-legacy database
    CREATE USER assurance WITH PASSWORD 'assurance123';
    CREATE DATABASE assurance_legacy OWNER assurance;
    GRANT ALL PRIVILEGES ON DATABASE assurance_legacy TO assurance;
EOSQL

echo "Multiple databases created successfully"