-- Journal des SMS (cahier-des-charges.md §16).
--
-- Les notifications n'étaient jamais envoyées : la passerelle par défaut écrivait dans les
-- logs du serveur. Or au Mali le SMS est le canal qui atteint réellement les parents, et
-- toute la promesse « communication avec les familles » reposait dessus.
--
-- Chaque envoi est tracé, réussi comme échoué. Deux raisons : un SMS se facture à l'unité,
-- l'établissement doit pouvoir rapprocher sa facture opérateur ; et quand un parent affirme
-- n'avoir rien reçu, il faut pouvoir répondre autre chose que « c'est parti, normalement ».

CREATE TABLE sms_messages (
    id             BIGSERIAL PRIMARY KEY,
    school_id      BIGINT       NOT NULL REFERENCES tenants (id),

    -- Numéro tel qu'il a été composé, après normalisation au format international.
    recipient      VARCHAR(32)  NOT NULL,

    -- Le corps réellement envoyé, pas le modèle : c'est lui qui fait foi en cas de litige.
    body           TEXT         NOT NULL,

    notification_type VARCHAR(32),

    -- PENDING / SENT / FAILED. Un SMS reste PENDING si l'appel au fournisseur n'aboutit pas
    -- à une réponse exploitable — mieux vaut un état indéterminé qu'un faux « envoyé ».
    status         VARCHAR(16)  NOT NULL,

    provider       VARCHAR(32)  NOT NULL,
    provider_reference VARCHAR(128),
    failure_reason VARCHAR(500),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX sms_messages_school_created_idx ON sms_messages (school_id, created_at DESC);

ALTER TABLE sms_messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE sms_messages FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON sms_messages
    USING (school_id = current_setting('app.tenant_id', true)::bigint);
