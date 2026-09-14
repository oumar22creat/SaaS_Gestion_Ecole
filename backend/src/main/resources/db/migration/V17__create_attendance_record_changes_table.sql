-- Historique des modifications d'une feuille d'appel (cahier-des-charges.md §10) : un
-- instantané de l'état PRÉCÉDENT à chaque modification (statut/motif/justificatif/commentaire).
CREATE TABLE attendance_record_changes (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    school_id             BIGINT      NOT NULL REFERENCES tenants (id),
    attendance_record_id  BIGINT      NOT NULL REFERENCES attendance_records (id),
    previous_status       VARCHAR(16) NOT NULL,
    previous_reason       VARCHAR(255),
    previous_justified    BOOLEAN     NOT NULL,
    previous_comment      VARCHAR(1000),
    changed_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_attendance_record_changes_record_id ON attendance_record_changes (attendance_record_id);

ALTER TABLE attendance_record_changes ENABLE ROW LEVEL SECURITY;
ALTER TABLE attendance_record_changes FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_policy ON attendance_record_changes
    USING (school_id = current_setting('app.tenant_id', true)::bigint);
