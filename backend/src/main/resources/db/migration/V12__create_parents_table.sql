-- Parents/tuteurs (cahier-des-charges.md §7) — pas de compte de connexion pour ce MVP (voir
-- V7, même remarque que pour teachers).
CREATE TABLE parents (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    school_id  BIGINT       NOT NULL REFERENCES tenants (id),
    first_name VARCHAR(100) NOT NULL,
    last_name  VARCHAR(100) NOT NULL,
    email      VARCHAR(255),
    phone      VARCHAR(32),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_parents_school_id ON parents (school_id);

ALTER TABLE parents ENABLE ROW LEVEL SECURITY;
ALTER TABLE parents FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_policy ON parents
    USING (school_id = current_setting('app.tenant_id', true)::bigint);
