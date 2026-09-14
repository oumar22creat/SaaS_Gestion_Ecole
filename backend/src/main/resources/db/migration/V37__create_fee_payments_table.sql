-- Paiement (partiel ou total) d'une facture de frais de scolarité (cahier-des-charges.md
-- §19.4/§4.3). Saisi manuellement par le personnel : aucun fournisseur mobile money n'est
-- câblé (bloquant identifié, voir docs/ARCHITECTURE.md ADR-024/"Points ouverts") — method
-- inclut MOBILE_MONEY pour permettre de tracer un paiement reçu hors-ligne via ce canal, pas
-- pour déclencher un paiement en ligne réel.
CREATE TABLE fee_payments (
    id                  BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    school_id           BIGINT      NOT NULL REFERENCES tenants (id),
    invoice_id          BIGINT      NOT NULL REFERENCES student_fee_invoices (id),
    amount_cents        BIGINT      NOT NULL,
    method              VARCHAR(16) NOT NULL,
    reference           VARCHAR(100),
    recorded_by_user_id BIGINT      NOT NULL REFERENCES users (id),
    paid_at             TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_fee_payments_school_id ON fee_payments (school_id);
CREATE INDEX idx_fee_payments_invoice_id ON fee_payments (invoice_id);

ALTER TABLE fee_payments ENABLE ROW LEVEL SECURITY;
ALTER TABLE fee_payments FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_policy ON fee_payments
    USING (school_id = current_setting('app.tenant_id', true)::bigint);
