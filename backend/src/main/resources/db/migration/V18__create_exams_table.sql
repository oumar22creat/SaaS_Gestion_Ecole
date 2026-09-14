-- Évaluations (cahier-des-charges.md §11).
CREATE TABLE exams (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    school_id       BIGINT       NOT NULL REFERENCES tenants (id),
    school_class_id BIGINT       NOT NULL REFERENCES school_classes (id),
    subject_id      BIGINT       NOT NULL REFERENCES subjects (id),
    label           VARCHAR(255) NOT NULL,
    max_score       DOUBLE PRECISION NOT NULL DEFAULT 20,
    coefficient     INTEGER      NOT NULL DEFAULT 1,
    exam_date       DATE         NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_exams_school_id ON exams (school_id);
CREATE INDEX idx_exams_subject_id ON exams (subject_id);
CREATE INDEX idx_exams_class_subject ON exams (school_class_id, subject_id);

ALTER TABLE exams ENABLE ROW LEVEL SECURITY;
ALTER TABLE exams FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_policy ON exams
    USING (school_id = current_setting('app.tenant_id', true)::bigint);
