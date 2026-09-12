-- Première table métier scopée par établissement (voir CLAUDE.md règle 1). Email unique par
-- établissement seulement (pas globalement) : un même utilisateur peut avoir un compte dans
-- deux établissements différents (voir cahier-des-charges.md §21).
CREATE TABLE users (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    school_id     BIGINT       NOT NULL REFERENCES tenants (id),
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name    VARCHAR(100) NOT NULL,
    last_name     VARCHAR(100) NOT NULL,
    role          VARCHAR(32)  NOT NULL,
    active        BOOLEAN      NOT NULL DEFAULT true,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (school_id, email)
);

CREATE INDEX idx_users_school_id ON users (school_id);

-- Row-Level Security : filet de sécurité en plus du filtre applicatif (Hibernate Filter),
-- voir docs/ARCHITECTURE.md ADR-001 et CLAUDE.md règle 2.
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE users FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_policy ON users
    USING (school_id = current_setting('app.tenant_id', true)::bigint);
