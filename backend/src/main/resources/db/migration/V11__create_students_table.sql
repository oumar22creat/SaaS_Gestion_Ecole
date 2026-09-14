-- Élèves (cahier-des-charges.md §7). Champs volontairement minimaux pour ce MVP (pas de
-- photo/dossier administratif/pièces justificatives — module document/ de Phase 2, voir
-- docs/ARCHITECTURE.md ADR-010).
CREATE TABLE students (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    school_id       BIGINT       NOT NULL REFERENCES tenants (id),
    student_number  VARCHAR(64)  NOT NULL,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    birth_date      DATE,
    gender          VARCHAR(16),
    school_class_id BIGINT REFERENCES school_classes (id),
    active          BOOLEAN      NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (school_id, student_number)
);

CREATE INDEX idx_students_school_id ON students (school_id);
CREATE INDEX idx_students_school_class_id ON students (school_class_id);

ALTER TABLE students ENABLE ROW LEVEL SECURITY;
ALTER TABLE students FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_policy ON students
    USING (school_id = current_setting('app.tenant_id', true)::bigint);
