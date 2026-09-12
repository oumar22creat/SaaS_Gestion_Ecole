#!/bin/sh
# Exécuté automatiquement par l'image officielle postgres au premier démarrage du volume
# (docker-entrypoint-initdb.d). Crée un rôle applicatif restreint, NON superutilisateur,
# distinct du rôle utilisé par Flyway pour les migrations (POSTGRES_USER/$DB_ADMIN_USER).
#
# Raison : PostgreSQL ignore TOUJOURS les politiques Row-Level Security pour un
# superutilisateur, y compris avec FORCE ROW LEVEL SECURITY (voir docs/ARCHITECTURE.md
# ADR-001) — si l'application se connectait avec le même rôle que Flyway (superutilisateur
# via POSTGRES_USER), RLS ne protégerait jamais rien. Voir TenantIsolationTest#
# rowLevelSecurityAloneBlocksAccessForANonSuperuserRole pour la preuve du mécanisme.
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE ROLE "${DB_APP_USER}" LOGIN PASSWORD '${DB_APP_PASSWORD}' NOSUPERUSER;
    GRANT CONNECT ON DATABASE "${POSTGRES_DB}" TO "${DB_APP_USER}";
    GRANT USAGE ON SCHEMA public TO "${DB_APP_USER}";
    ALTER DEFAULT PRIVILEGES FOR ROLE "${POSTGRES_USER}" IN SCHEMA public
        GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO "${DB_APP_USER}";
    ALTER DEFAULT PRIVILEGES FOR ROLE "${POSTGRES_USER}" IN SCHEMA public
        GRANT USAGE, SELECT ON SEQUENCES TO "${DB_APP_USER}";
EOSQL
