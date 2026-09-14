-- Notes (cahier-des-charges.md §11) — score NULL autorisé pour représenter une absence à
-- l'évaluation (voir colonne absent), pas une note de zéro.
CREATE TABLE grades (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    school_id  BIGINT       NOT NULL REFERENCES tenants (id),
    exam_id    BIGINT       NOT NULL REFERENCES exams (id),
    student_id BIGINT       NOT NULL REFERENCES students (id),
    score      DOUBLE PRECISION,
    absent     BOOLEAN      NOT NULL DEFAULT false,
    comment    VARCHAR(1000),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (exam_id, student_id)
);

CREATE INDEX idx_grades_school_id ON grades (school_id);
CREATE INDEX idx_grades_exam_id ON grades (exam_id);
CREATE INDEX idx_grades_student_id ON grades (student_id);

ALTER TABLE grades ENABLE ROW LEVEL SECURITY;
ALTER TABLE grades FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_policy ON grades
    USING (school_id = current_setting('app.tenant_id', true)::bigint);
