-- Association élève/parent (cahier-des-charges.md §7) : plusieurs parents par élève, un
-- parent peut avoir plusieurs enfants — table de liaison classique.
CREATE TABLE student_parents (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    school_id        BIGINT      NOT NULL REFERENCES tenants (id),
    student_id       BIGINT      NOT NULL REFERENCES students (id),
    parent_id        BIGINT      NOT NULL REFERENCES parents (id),
    relationship     VARCHAR(32) NOT NULL,
    primary_contact  BOOLEAN     NOT NULL DEFAULT false,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (student_id, parent_id)
);

CREATE INDEX idx_student_parents_school_id ON student_parents (school_id);
CREATE INDEX idx_student_parents_student_id ON student_parents (student_id);
CREATE INDEX idx_student_parents_parent_id ON student_parents (parent_id);

ALTER TABLE student_parents ENABLE ROW LEVEL SECURITY;
ALTER TABLE student_parents FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_policy ON student_parents
    USING (school_id = current_setting('app.tenant_id', true)::bigint);
