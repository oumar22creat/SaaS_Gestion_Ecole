-- Facturation du service de transport (cahier-des-charges.md §19.2) : forfait périodique
-- (pas "à la consommation" comme la cantine — le cahier ne le précise pas ici), montant
-- donné à la génération plutôt qu'une grille tarifaire persistée, même simplification que
-- pour la cantine (voir ADR-025/ADR-027).
CREATE TABLE transport_invoices (
    id                BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    school_id         BIGINT      NOT NULL REFERENCES tenants (id),
    student_id        BIGINT      NOT NULL REFERENCES students (id),
    period_from       DATE        NOT NULL,
    period_to         DATE        NOT NULL,
    amount_due_cents  BIGINT      NOT NULL,
    status            VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    issued_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_transport_invoices_school_id ON transport_invoices (school_id);
CREATE INDEX idx_transport_invoices_student_id ON transport_invoices (student_id);

ALTER TABLE transport_invoices ENABLE ROW LEVEL SECURITY;
ALTER TABLE transport_invoices FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_policy ON transport_invoices
    USING (school_id = current_setting('app.tenant_id', true)::bigint);
