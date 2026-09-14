-- Incidents disciplinaires (cahier-des-charges.md §17). Rattachés à une classe ; les élèves
-- concernés sont dans incident_students (un incident peut impliquer plusieurs élèves).
CREATE TABLE incidents (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    school_id           BIGINT      NOT NULL REFERENCES tenants (id),
    school_class_id     BIGINT      NOT NULL REFERENCES school_classes (id),
    occurred_at          DATE        NOT NULL,
    severity            VARCHAR(16) NOT NULL,
    description         VARCHAR(2000) NOT NULL,
    reported_by_user_id BIGINT      NOT NULL REFERENCES users (id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_incidents_school_id ON incidents (school_id);
CREATE INDEX idx_incidents_class_id ON incidents (school_class_id);

ALTER TABLE incidents ENABLE ROW LEVEL SECURITY;
ALTER TABLE incidents FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_policy ON incidents
    USING (school_id = current_setting('app.tenant_id', true)::bigint);
