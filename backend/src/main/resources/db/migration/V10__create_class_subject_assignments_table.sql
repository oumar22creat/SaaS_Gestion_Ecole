-- Affectation enseignant/classe/matière (cahier-des-charges.md §8) : un seul enseignant par
-- couple (classe, matière) pour ce MVP — réaffecter remplace l'enseignant existant plutôt que
-- de gérer la co-intervention (non demandée par docs/ROADMAP.md 1.5).
CREATE TABLE class_subject_assignments (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    school_id   BIGINT      NOT NULL REFERENCES tenants (id),
    class_id    BIGINT      NOT NULL REFERENCES school_classes (id),
    subject_id  BIGINT      NOT NULL REFERENCES subjects (id),
    teacher_id  BIGINT      NOT NULL REFERENCES teachers (id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (class_id, subject_id)
);

CREATE INDEX idx_class_subject_assignments_school_id ON class_subject_assignments (school_id);

ALTER TABLE class_subject_assignments ENABLE ROW LEVEL SECURITY;
ALTER TABLE class_subject_assignments FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_policy ON class_subject_assignments
    USING (school_id = current_setting('app.tenant_id', true)::bigint);
