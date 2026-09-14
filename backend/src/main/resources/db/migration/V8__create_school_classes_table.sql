-- Classes (cahier-des-charges.md §8). Pas de notion d'année scolaire pour ce MVP (non listée
-- dans docs/ROADMAP.md 1.5 — simplification assumée, voir docs/ARCHITECTURE.md ADR-010) :
-- une seule génération de classes "courantes" par établissement.
CREATE TABLE school_classes (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    school_id        BIGINT       NOT NULL REFERENCES tenants (id),
    name             VARCHAR(100) NOT NULL,
    head_teacher_id  BIGINT REFERENCES teachers (id),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_school_classes_school_id ON school_classes (school_id);

ALTER TABLE school_classes ENABLE ROW LEVEL SECURITY;
ALTER TABLE school_classes FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_policy ON school_classes
    USING (school_id = current_setting('app.tenant_id', true)::bigint);
