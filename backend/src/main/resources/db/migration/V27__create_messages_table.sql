-- Messages d'une conversation (cahier-des-charges.md §15), pièce jointe optionnelle
-- référençant le module documents (ADR-017).
CREATE TABLE messages (
    id                     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    school_id              BIGINT       NOT NULL REFERENCES tenants (id),
    conversation_id        BIGINT       NOT NULL REFERENCES conversations (id),
    sender_id              BIGINT       NOT NULL REFERENCES users (id),
    content                VARCHAR(4000) NOT NULL,
    attachment_document_id BIGINT REFERENCES documents (id),
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_messages_school_id ON messages (school_id);
CREATE INDEX idx_messages_conversation_id ON messages (conversation_id, created_at);

ALTER TABLE messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE messages FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_policy ON messages
    USING (school_id = current_setting('app.tenant_id', true)::bigint);
