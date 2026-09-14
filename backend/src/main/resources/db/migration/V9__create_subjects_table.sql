-- Matières (cahier-des-charges.md §8).
CREATE TABLE subjects (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    school_id   BIGINT       NOT NULL REFERENCES tenants (id),
    name        VARCHAR(100) NOT NULL,
    code        VARCHAR(32)  NOT NULL,
    coefficient INTEGER      NOT NULL DEFAULT 1,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (school_id, code)
);

CREATE INDEX idx_subjects_school_id ON subjects (school_id);

ALTER TABLE subjects ENABLE ROW LEVEL SECURITY;
ALTER TABLE subjects FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_policy ON subjects
    USING (school_id = current_setting('app.tenant_id', true)::bigint);
