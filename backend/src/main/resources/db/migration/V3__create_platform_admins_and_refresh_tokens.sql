-- Comptes Super-Administrateur : hors tenant (voir docs/ARCHITECTURE.md ADR-008), pas de
-- school_id, pas de RLS (rien à cloisonner par établissement ici).
CREATE TABLE platform_admins (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name    VARCHAR(100) NOT NULL,
    last_name     VARCHAR(100) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Jetons de rafraîchissement, communs aux comptes users et platform_admins (voir
-- com.schoolsaas.auth.RefreshToken) : pas de school_id, pas de RLS, pas de FK vers tenants
-- (subject_type + subject_id référencent l'une ou l'autre table selon le cas).
CREATE TABLE refresh_tokens (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    token_hash  VARCHAR(64) NOT NULL UNIQUE,
    subject_type VARCHAR(32) NOT NULL,
    subject_id  BIGINT      NOT NULL,
    tenant_id   BIGINT,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked     BOOLEAN     NOT NULL DEFAULT false,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens (token_hash);
