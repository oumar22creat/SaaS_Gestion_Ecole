-- Templates de notification/e-mail personnalisables par établissement (cahier §2.4,
-- ROADMAP.md 3.7, dernier item). Aucune ligne pour un (school_id, type) donné = message par
-- défaut du module appelant inchangé (voir docs/ARCHITECTURE.md ADR-028) : même principe de
-- "ligne absente = comportement par défaut" que notification_preferences.
CREATE TABLE notification_templates (
    id              BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    school_id       BIGINT       NOT NULL REFERENCES tenants (id),
    type            VARCHAR(32)  NOT NULL,
    title_override  VARCHAR(255),
    body_template   VARCHAR(2000),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (school_id, type)
);

CREATE INDEX idx_notification_templates_school_id ON notification_templates (school_id);

ALTER TABLE notification_templates ENABLE ROW LEVEL SECURITY;
ALTER TABLE notification_templates FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_policy ON notification_templates
    USING (school_id = current_setting('app.tenant_id', true)::bigint);
