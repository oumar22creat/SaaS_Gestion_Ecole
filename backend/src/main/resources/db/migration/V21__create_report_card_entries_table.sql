-- Ligne "matière" d'un bulletin — moyenne et coefficient figés au moment de la génération
-- (un changement ultérieur du coefficient de la matière ne doit pas modifier un bulletin déjà
-- généré), appréciation de l'enseignant de la matière (cahier-des-charges.md §12).
CREATE TABLE report_card_entries (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    school_id        BIGINT      NOT NULL REFERENCES tenants (id),
    report_card_id   BIGINT      NOT NULL REFERENCES report_cards (id),
    subject_id       BIGINT      NOT NULL REFERENCES subjects (id),
    average          DOUBLE PRECISION,
    coefficient      INTEGER     NOT NULL,
    teacher_comment  VARCHAR(1000),
    UNIQUE (report_card_id, subject_id)
);

CREATE INDEX idx_report_card_entries_school_id ON report_card_entries (school_id);
CREATE INDEX idx_report_card_entries_report_card_id ON report_card_entries (report_card_id);

ALTER TABLE report_card_entries ENABLE ROW LEVEL SECURITY;
ALTER TABLE report_card_entries FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_policy ON report_card_entries
    USING (school_id = current_setting('app.tenant_id', true)::bigint);
