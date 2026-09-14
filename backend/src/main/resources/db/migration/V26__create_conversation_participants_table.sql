-- Participants d'une conversation — `last_read_at` porte la confirmation de lecture (cahier
-- §15) : tous les messages antérieurs à cette date sont considérés lus par ce participant.
CREATE TABLE conversation_participants (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    school_id       BIGINT      NOT NULL REFERENCES tenants (id),
    conversation_id BIGINT      NOT NULL REFERENCES conversations (id),
    user_id         BIGINT      NOT NULL REFERENCES users (id),
    last_read_at    TIMESTAMPTZ,
    UNIQUE (conversation_id, user_id)
);

CREATE INDEX idx_conversation_participants_school_id ON conversation_participants (school_id);
CREATE INDEX idx_conversation_participants_conversation_id ON conversation_participants (conversation_id);
CREATE INDEX idx_conversation_participants_user_id ON conversation_participants (user_id);

ALTER TABLE conversation_participants ENABLE ROW LEVEL SECURITY;
ALTER TABLE conversation_participants FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_policy ON conversation_participants
    USING (school_id = current_setting('app.tenant_id', true)::bigint);
