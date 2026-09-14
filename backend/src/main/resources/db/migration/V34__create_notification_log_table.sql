-- Journal des notifications réellement envoyées (cahier-des-charges.md §18 : "usage global —
-- notifications envoyées", tableau de bord Super-Admin). Entité plateforme, pas de school_id
-- RLS-protégé (comme subscriptions/invoices, voir V5/V6) : c'est un compteur d'usage global
-- pour le Super-Admin, pas une donnée métier scopée établissement à isoler par tenant.
-- tenant_id est nullable : NotificationDispatcher le déduit de TenantContext, qui n'est
-- positionné que dans le fil d'une requête HTTP authentifiée (voir TenantContextInterceptor) ;
-- un appel hors requête (tests, futur job planifié) loggue quand même l'envoi, sans tenant
-- connu, plutôt que d'échouer ou de fausser le compteur global.
CREATE TABLE notification_log (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id         BIGINT      REFERENCES tenants (id),
    type              VARCHAR(32) NOT NULL,
    recipient_count   INTEGER     NOT NULL,
    sent_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_notification_log_sent_at ON notification_log (sent_at);
