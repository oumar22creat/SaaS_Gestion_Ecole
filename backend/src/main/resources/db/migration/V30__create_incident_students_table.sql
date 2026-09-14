-- Élèves impliqués dans un incident (cahier-des-charges.md §17 : "élève(s) concerné(s)") —
-- même pattern many-to-many que conversation_participants (V26).
CREATE TABLE incident_students (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    school_id   BIGINT NOT NULL REFERENCES tenants (id),
    incident_id BIGINT NOT NULL REFERENCES incidents (id),
    student_id  BIGINT NOT NULL REFERENCES students (id),
    UNIQUE (incident_id, student_id)
);

CREATE INDEX idx_incident_students_school_id ON incident_students (school_id);
CREATE INDEX idx_incident_students_incident_id ON incident_students (incident_id);
CREATE INDEX idx_incident_students_student_id ON incident_students (student_id);

ALTER TABLE incident_students ENABLE ROW LEVEL SECURITY;
ALTER TABLE incident_students FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_policy ON incident_students
    USING (school_id = current_setting('app.tenant_id', true)::bigint);
