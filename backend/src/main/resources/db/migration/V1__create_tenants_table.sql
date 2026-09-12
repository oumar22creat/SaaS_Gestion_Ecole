-- Table racine du multi-tenant (voir docs/ARCHITECTURE.md ADR-001). Volontairement pas
-- scopée par school_id : c'est elle-même la définition d'un établissement.
CREATE TABLE tenants (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    subdomain   VARCHAR(63)  NOT NULL UNIQUE,
    status      VARCHAR(32)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
