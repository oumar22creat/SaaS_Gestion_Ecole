-- Observations positives/négatives (cahier-des-charges.md §17), indépendantes d'un incident
-- (contrairement aux sanctions) — alimentent l'historique disciplinaire d'un élève.
CREATE TABLE observations (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    school_id        BIGINT      NOT NULL REFERENCES tenants (id),
    student_id       BIGINT      NOT NULL REFERENCES students (id),
    positive         BOOLEAN     NOT NULL,
    description      VARCHAR(2000) NOT NULL,
    author_user_id   BIGINT      NOT NULL REFERENCES users (id),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_observations_school_id ON observations (school_id);
CREATE INDEX idx_observations_student_id ON observations (student_id);

ALTER TABLE observations ENABLE ROW LEVEL SECURITY;
ALTER TABLE observations FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_policy ON observations
    USING (school_id = current_setting('app.tenant_id', true)::bigint);
