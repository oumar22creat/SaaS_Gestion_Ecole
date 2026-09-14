-- Bulletins scolaires (cahier-des-charges.md §12). `period_label` est un texte libre
-- ("Trimestre 1"...) : pas d'entité "année scolaire/période" construite (simplification
-- assumée depuis ADR-010) — from/to délimitent la période pour le calcul des absences.
CREATE TABLE report_cards (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    school_id        BIGINT       NOT NULL REFERENCES tenants (id),
    student_id       BIGINT       NOT NULL REFERENCES students (id),
    school_class_id  BIGINT       NOT NULL REFERENCES school_classes (id),
    period_label     VARCHAR(100) NOT NULL,
    period_from      DATE         NOT NULL,
    period_to        DATE         NOT NULL,
    general_average  DOUBLE PRECISION,
    general_comment  VARCHAR(2000),
    council_decision VARCHAR(500),
    absence_count    INTEGER      NOT NULL DEFAULT 0,
    late_count       INTEGER      NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (student_id, period_label)
);

CREATE INDEX idx_report_cards_school_id ON report_cards (school_id);
CREATE INDEX idx_report_cards_class_id ON report_cards (school_class_id);

ALTER TABLE report_cards ENABLE ROW LEVEL SECURITY;
ALTER TABLE report_cards FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_policy ON report_cards
    USING (school_id = current_setting('app.tenant_id', true)::bigint);
