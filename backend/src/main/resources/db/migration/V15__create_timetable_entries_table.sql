-- Créneaux d'emploi du temps (cahier-des-charges.md §9) : répétition hebdomadaire simple
-- (day_of_week + heures), pas de date de début/fin ni d'exceptions ponctuelles pour ce MVP
-- (non listé dans docs/ROADMAP.md 1.6 — voir docs/ARCHITECTURE.md ADR-011).
CREATE TABLE timetable_entries (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    school_id       BIGINT      NOT NULL REFERENCES tenants (id),
    school_class_id BIGINT      NOT NULL REFERENCES school_classes (id),
    subject_id      BIGINT      NOT NULL REFERENCES subjects (id),
    teacher_id      BIGINT      NOT NULL REFERENCES teachers (id),
    room_id         BIGINT      NOT NULL REFERENCES rooms (id),
    day_of_week     VARCHAR(16) NOT NULL,
    start_time      TIME        NOT NULL,
    end_time        TIME        NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (start_time < end_time)
);

CREATE INDEX idx_timetable_entries_school_id ON timetable_entries (school_id);
CREATE INDEX idx_timetable_entries_class ON timetable_entries (school_class_id, day_of_week);
CREATE INDEX idx_timetable_entries_teacher ON timetable_entries (teacher_id, day_of_week);
CREATE INDEX idx_timetable_entries_room ON timetable_entries (room_id, day_of_week);

ALTER TABLE timetable_entries ENABLE ROW LEVEL SECURITY;
ALTER TABLE timetable_entries FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_policy ON timetable_entries
    USING (school_id = current_setting('app.tenant_id', true)::bigint);
