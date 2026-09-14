-- Paramétrage des notifications (cahier-des-charges.md §16) — par utilisateur (pas par
-- rôle : plus simple à raisonner avec le modèle User existant, voir docs/ARCHITECTURE.md
-- ADR-020). Aucune ligne pour un (user, type) donné = notification activée par défaut.
CREATE TABLE notification_preferences (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    school_id  BIGINT      NOT NULL REFERENCES tenants (id),
    user_id    BIGINT      NOT NULL REFERENCES users (id),
    type       VARCHAR(32) NOT NULL,
    enabled    BOOLEAN     NOT NULL DEFAULT true,
    UNIQUE (user_id, type)
);

CREATE INDEX idx_notification_preferences_school_id ON notification_preferences (school_id);
CREATE INDEX idx_notification_preferences_user_id ON notification_preferences (user_id);

ALTER TABLE notification_preferences ENABLE ROW LEVEL SECURITY;
ALTER TABLE notification_preferences FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_policy ON notification_preferences
    USING (school_id = current_setting('app.tenant_id', true)::bigint);
