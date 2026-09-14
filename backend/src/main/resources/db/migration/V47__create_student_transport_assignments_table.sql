-- Affectation d'un élève à un circuit/arrêt (cahier-des-charges.md §19.2). Une seule
-- affectation active par élève à la fois, comme school_class_id sur students.
CREATE TABLE student_transport_assignments (
    id            BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    school_id     BIGINT      NOT NULL REFERENCES tenants (id),
    student_id    BIGINT      NOT NULL UNIQUE REFERENCES students (id),
    bus_route_id  BIGINT      NOT NULL REFERENCES bus_routes (id),
    bus_stop_id   BIGINT      NOT NULL REFERENCES bus_stops (id),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_student_transport_assignments_school_id ON student_transport_assignments (school_id);
CREATE INDEX idx_student_transport_assignments_route_id ON student_transport_assignments (bus_route_id);

ALTER TABLE student_transport_assignments ENABLE ROW LEVEL SECURITY;
ALTER TABLE student_transport_assignments FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_policy ON student_transport_assignments
    USING (school_id = current_setting('app.tenant_id', true)::bigint);
