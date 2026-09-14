-- Convocations (cahier-des-charges.md §17) : élève et/ou parent convoqués pour un motif donné.
CREATE TABLE convocations (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    school_id           BIGINT      NOT NULL REFERENCES tenants (id),
    student_id          BIGINT      NOT NULL REFERENCES students (id),
    convene_parent       BOOLEAN     NOT NULL DEFAULT true,
    reason              VARCHAR(2000) NOT NULL,
    scheduled_at        TIMESTAMPTZ NOT NULL,
    status              VARCHAR(16) NOT NULL DEFAULT 'SCHEDULED',
    created_by_user_id  BIGINT      NOT NULL REFERENCES users (id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_convocations_school_id ON convocations (school_id);
CREATE INDEX idx_convocations_student_id ON convocations (student_id);

ALTER TABLE convocations ENABLE ROW LEVEL SECURITY;
ALTER TABLE convocations FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_policy ON convocations
    USING (school_id = current_setting('app.tenant_id', true)::bigint);
