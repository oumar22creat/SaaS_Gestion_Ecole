-- Abonnement d'un établissement — entité plateforme (voir V4, pas de school_id/RLS, comme
-- `tenants`/`plans` : c'est le Super-Admin/Stripe qui en sont propriétaires, pas un rôle
-- scopé à l'établissement). Un seul enregistrement par tenant pour le MVP (pas d'historique
-- de changement de plan, voir docs/ARCHITECTURE.md ADR-009).
CREATE TABLE subscriptions (
    id                     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id              BIGINT       NOT NULL UNIQUE REFERENCES tenants (id),
    plan_id                BIGINT       NOT NULL REFERENCES plans (id),
    status                 VARCHAR(16)  NOT NULL,
    trial_ends_at          TIMESTAMPTZ  NOT NULL,
    current_period_end     TIMESTAMPTZ,
    stripe_customer_id     VARCHAR(64),
    stripe_subscription_id VARCHAR(64) UNIQUE,
    payment_failed_at      TIMESTAMPTZ,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_subscriptions_status_trial_ends_at ON subscriptions (status, trial_ends_at);
CREATE INDEX idx_subscriptions_status_payment_failed_at ON subscriptions (status, payment_failed_at);
