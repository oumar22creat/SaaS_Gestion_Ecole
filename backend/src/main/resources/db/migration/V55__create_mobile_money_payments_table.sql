-- Paiement des frais de scolarité par mobile money (cahier §4.3). Jusqu'ici,
-- FeePaymentMethod.MOBILE_MONEY ne servait qu'à tracer un règlement reçu hors ligne : rien
-- ne permettait de déclencher un paiement, ni de rapprocher automatiquement la facture.
--
-- La table garde la trace de chaque tentative, y compris échouée. Sans elle, un rappel de
-- callback du fournisseur (ils réessaient) créditerait la facture plusieurs fois.

CREATE TABLE mobile_money_payments (
    id                 BIGSERIAL PRIMARY KEY,
    school_id          BIGINT       NOT NULL REFERENCES tenants (id),
    invoice_id         BIGINT       NOT NULL REFERENCES student_fee_invoices (id),
    amount_cents       BIGINT       NOT NULL CHECK (amount_cents > 0),
    payer_msisdn       VARCHAR(32)  NOT NULL,
    provider           VARCHAR(32)  NOT NULL,

    -- Notre référence, transmise au fournisseur et renvoyée dans le callback. Elle porte
    -- l'identifiant d'établissement (format OM-<school_id>-<aléa>) : le callback arrive sans
    -- en-tête ni jeton, il faut donc pouvoir poser le contexte tenant AVANT toute lecture,
    -- sinon la politique RLS ci-dessous masque la ligne qu'on cherche. Même principe que le
    -- client_reference_id de Stripe.
    reference          VARCHAR(64)  NOT NULL UNIQUE,

    -- Référence du fournisseur, connue seulement au retour.
    provider_reference VARCHAR(128),

    status             VARCHAR(16)  NOT NULL,
    failure_reason     VARCHAR(255),
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX mobile_money_payments_invoice_idx ON mobile_money_payments (school_id, invoice_id);

ALTER TABLE mobile_money_payments ENABLE ROW LEVEL SECURITY;
ALTER TABLE mobile_money_payments FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_policy ON mobile_money_payments
    USING (school_id = current_setting('app.tenant_id', true)::bigint);

-- Un paiement encaissé par l'opérateur n'a personne qui l'a saisi : la colonne devient
-- nullable, et un `recorded_by_user_id` vide se lit désormais comme « encaissement
-- automatique » plutôt que comme une donnée manquante.
ALTER TABLE fee_payments ALTER COLUMN recorded_by_user_id DROP NOT NULL;
