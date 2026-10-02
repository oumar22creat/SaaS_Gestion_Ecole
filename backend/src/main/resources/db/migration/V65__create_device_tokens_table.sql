-- Jetons d'appareil pour les notifications push (docs/NOTIFICATIONS-PUSH.md).
--
-- Brique manquante jusqu'ici : NotificationDispatcher sait à quels *utilisateurs* s'adresser,
-- mais rien ne reliait un utilisateur aux *appareils* qu'il porte. Sans cette table, brancher
-- FCM ne suffirait pas — la passerelle n'aurait aucune adresse où écrire.
--
-- Un utilisateur a plusieurs appareils (téléphone, tablette), et chaque appareil ne vaut que
-- pour un compte à la fois : d'où l'unicité sur (school_id, token) plutôt que sur user_id.
-- Enregistrer un jeton déjà connu en réaffecte donc la propriété. C'est délibéré et c'est le
-- cas qui compte : sur un téléphone partagé — celui d'une école, celui d'un foyer — un
-- enseignant se déconnecte, un parent se connecte, et les notifications du premier ne doivent
-- pas continuer d'arriver sur un écran que lit désormais le second.
--
-- L'unicité porte sur (school_id, token) et non sur le seul token : une même personne peut
-- avoir un compte dans deux établissements — un enseignant dont l'enfant est scolarisé
-- ailleurs. Deux lignes coexistent alors légitimement pour le même appareil. Une unicité
-- globale les ferait entrer en collision, et la politique RLS masquerait la ligne fautive :
-- l'erreur remontée serait incompréhensible.
CREATE TABLE device_tokens (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    school_id    BIGINT        NOT NULL REFERENCES tenants (id),
    user_id      BIGINT        NOT NULL REFERENCES users (id),
    -- 4096 : FCM ne publie aucune borne et a déjà rallongé ses jetons par le passé. Tronquer
    -- un jeton ne lèverait pas d'erreur, il rendrait seulement l'appareil injoignable.
    token        VARCHAR(4096) NOT NULL,
    platform     VARCHAR(16)   NOT NULL,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    -- Rafraîchi à chaque enregistrement : permet de purger les appareils qu'on n'a plus vus
    -- depuis des mois, sans attendre que FCM les déclare inconnus.
    last_seen_at TIMESTAMPTZ   NOT NULL DEFAULT now(),
    UNIQUE (school_id, token)
);

CREATE INDEX idx_device_tokens_school_id ON device_tokens (school_id);
-- L'accès de loin le plus fréquent : « les appareils de ces destinataires », à chaque envoi.
CREATE INDEX idx_device_tokens_user_id ON device_tokens (user_id);

ALTER TABLE device_tokens ENABLE ROW LEVEL SECURITY;
ALTER TABLE device_tokens FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_policy ON device_tokens
    USING (school_id = current_setting('app.tenant_id', true)::bigint);
