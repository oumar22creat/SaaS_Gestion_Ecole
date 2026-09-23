-- Journal des actions de la console plateforme (cahier-des-charges.md §5).
--
-- Suspendre un établissement, prolonger un essai ou changer son plan engage l'éditeur vis-à-vis
-- d'un client : ces gestes ne doivent jamais être anonymes. Sans ce journal, personne ne peut
-- répondre six mois plus tard à « qui a coupé cette école, et pourquoi ».
--
-- Pas de Row-Level Security : la table appartient à la plateforme, pas à un établissement.
-- Elle n'est lisible que par un Super-Administrateur.

CREATE TABLE platform_admin_actions (
    id                BIGSERIAL PRIMARY KEY,
    platform_admin_id BIGINT      NOT NULL REFERENCES platform_admins (id),

    -- Établissement concerné. Nullable : certaines actions ne visent pas un établissement
    -- (création d'un compte plateforme, par exemple).
    tenant_id         BIGINT      REFERENCES tenants (id),

    action            VARCHAR(48) NOT NULL,

    -- Ce qui a changé, en clair : « TRIAL → SUSPENDED », « ESSENTIEL → PREMIUM ».
    detail            VARCHAR(255),

    reason            VARCHAR(500),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX platform_admin_actions_tenant_idx ON platform_admin_actions (tenant_id, created_at DESC);
CREATE INDEX platform_admin_actions_created_idx ON platform_admin_actions (created_at DESC);
