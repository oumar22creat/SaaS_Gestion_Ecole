-- Absences et retards (cahier-des-charges.md §10). Granularité "par jour" pour ce MVP (pas
-- par créneau/cours — voir docs/ARCHITECTURE.md ADR-012), un seul statut par élève et par
-- jour.
CREATE TABLE attendance_records (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    school_id        BIGINT      NOT NULL REFERENCES tenants (id),
    student_id       BIGINT      NOT NULL REFERENCES students (id),
    school_class_id  BIGINT      NOT NULL REFERENCES school_classes (id),
    date             DATE        NOT NULL,
    status           VARCHAR(16) NOT NULL,
    reason           VARCHAR(255),
    justified        BOOLEAN     NOT NULL DEFAULT false,
    comment          VARCHAR(1000),
    parent_notified  BOOLEAN     NOT NULL DEFAULT false,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (student_id, date)
);

CREATE INDEX idx_attendance_records_school_id ON attendance_records (school_id);
CREATE INDEX idx_attendance_records_class_date ON attendance_records (school_class_id, date);

ALTER TABLE attendance_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE attendance_records FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_policy ON attendance_records
    USING (school_id = current_setting('app.tenant_id', true)::bigint);
