-- Paiement (partiel ou total) d'une facture de transport — même principe que
-- fee_payments (V37)/canteen_payments (V41).
CREATE TABLE transport_payments (
    id                  BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    school_id           BIGINT      NOT NULL REFERENCES tenants (id),
    invoice_id          BIGINT      NOT NULL REFERENCES transport_invoices (id),
    amount_cents        BIGINT      NOT NULL,
    method              VARCHAR(16) NOT NULL,
    reference           VARCHAR(100),
    recorded_by_user_id BIGINT      NOT NULL REFERENCES users (id),
    paid_at             TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_transport_payments_school_id ON transport_payments (school_id);
CREATE INDEX idx_transport_payments_invoice_id ON transport_payments (invoice_id);

ALTER TABLE transport_payments ENABLE ROW LEVEL SECURITY;
ALTER TABLE transport_payments FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_policy ON transport_payments
    USING (school_id = current_setting('app.tenant_id', true)::bigint);
