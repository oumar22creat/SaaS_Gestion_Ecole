-- Paiement (partiel ou total) d'une facture de cantine — même principe que fee_payments
-- (V37), méthode et suivi des impayés (cahier-des-charges.md §19.1).
CREATE TABLE canteen_payments (
    id                  BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    school_id           BIGINT      NOT NULL REFERENCES tenants (id),
    invoice_id          BIGINT      NOT NULL REFERENCES canteen_invoices (id),
    amount_cents        BIGINT      NOT NULL,
    method              VARCHAR(16) NOT NULL,
    reference           VARCHAR(100),
    recorded_by_user_id BIGINT      NOT NULL REFERENCES users (id),
    paid_at             TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_canteen_payments_school_id ON canteen_payments (school_id);
CREATE INDEX idx_canteen_payments_invoice_id ON canteen_payments (invoice_id);

ALTER TABLE canteen_payments ENABLE ROW LEVEL SECURITY;
ALTER TABLE canteen_payments FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_policy ON canteen_payments
    USING (school_id = current_setting('app.tenant_id', true)::bigint);
