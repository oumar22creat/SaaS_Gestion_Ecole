-- Sanctions et punitions (cahier-des-charges.md §17), toujours rattachées à un incident et à
-- un élève précis (deux élèves d'un même incident peuvent recevoir des sanctions différentes).
CREATE TABLE sanctions (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    school_id          BIGINT      NOT NULL REFERENCES tenants (id),
    incident_id        BIGINT      NOT NULL REFERENCES incidents (id),
    student_id         BIGINT      NOT NULL REFERENCES students (id),
    type               VARCHAR(16) NOT NULL,
    duration_days      INTEGER,
    description        VARCHAR(2000),
    decided_by_user_id BIGINT      NOT NULL REFERENCES users (id),
    decided_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_sanctions_school_id ON sanctions (school_id);
CREATE INDEX idx_sanctions_incident_id ON sanctions (incident_id);
CREATE INDEX idx_sanctions_student_id ON sanctions (student_id);

ALTER TABLE sanctions ENABLE ROW LEVEL SECURITY;
ALTER TABLE sanctions FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_policy ON sanctions
    USING (school_id = current_setting('app.tenant_id', true)::bigint);
