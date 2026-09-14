-- Historique de facturation — entité plateforme (voir V5), consultable en lecture par les
-- rôles Comptable/Direction de l'établissement (cahier-des-charges.md §4.3), filtré
-- manuellement par tenant_id dans le repository (pas de filtre Hibernate ici, ces entités ne
-- sont pas scopées school_id — voir docs/ARCHITECTURE.md ADR-009).
CREATE TABLE invoices (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id           BIGINT       NOT NULL REFERENCES tenants (id),
    subscription_id     BIGINT       NOT NULL REFERENCES subscriptions (id),
    stripe_invoice_id   VARCHAR(64)  NOT NULL UNIQUE,
    amount_due          INTEGER      NOT NULL,
    currency            VARCHAR(3)   NOT NULL,
    status              VARCHAR(16)  NOT NULL,
    period_start        TIMESTAMPTZ,
    period_end          TIMESTAMPTZ,
    hosted_invoice_url  VARCHAR(500),
    paid_at             TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_invoices_tenant_id ON invoices (tenant_id);
