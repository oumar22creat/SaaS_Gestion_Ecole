-- Grille tarifaire des frais de scolarité par classe (cahier-des-charges.md §19.4). Pas de
-- notion de "niveau" distincte de la classe (aucun concept de ce type modélisé ailleurs,
-- voir ADR-010/ADR-024) : une grille s'applique à une classe donnée ; si plusieurs classes
-- partagent un même niveau, l'établissement crée une grille identique sur chacune.
-- due_date porte l'échéancier : plusieurs lignes (labels différents, échéances différentes)
-- pour une même classe représentent les tranches de paiement.
CREATE TABLE fee_schedules (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    school_id        BIGINT       NOT NULL REFERENCES tenants (id),
    school_class_id  BIGINT       NOT NULL REFERENCES school_classes (id),
    label            VARCHAR(255) NOT NULL,
    amount_cents     BIGINT       NOT NULL,
    currency         VARCHAR(3)   NOT NULL DEFAULT 'XOF',
    due_date         DATE         NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_fee_schedules_school_id ON fee_schedules (school_id);
CREATE INDEX idx_fee_schedules_class_id ON fee_schedules (school_class_id);

ALTER TABLE fee_schedules ENABLE ROW LEVEL SECURITY;
ALTER TABLE fee_schedules FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_policy ON fee_schedules
    USING (school_id = current_setting('app.tenant_id', true)::bigint);
