-- Messagerie interne (cahier-des-charges.md §15). Une "annonce" est une conversation comme
-- une autre (is_announcement=true), diffusée à tous les utilisateurs actifs du tenant à la
-- création (voir docs/ARCHITECTURE.md ADR-019) — pas un mécanisme séparé.
CREATE TABLE conversations (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    school_id         BIGINT       NOT NULL REFERENCES tenants (id),
    title             VARCHAR(255),
    is_announcement   BOOLEAN      NOT NULL DEFAULT false,
    created_by_user_id BIGINT      NOT NULL REFERENCES users (id),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_conversations_school_id ON conversations (school_id);

ALTER TABLE conversations ENABLE ROW LEVEL SECURITY;
ALTER TABLE conversations FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_policy ON conversations
    USING (school_id = current_setting('app.tenant_id', true)::bigint);
