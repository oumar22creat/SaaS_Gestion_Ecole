-- Catalogue des plans d'abonnement — entité plateforme, pas scopée par tenant (voir
-- docs/DATA_MODEL.md : plans est listé "hors tenant", au même titre que tenants lui-même).
--
-- price_cents est exprimé dans la plus petite unité de la devise. XOF (Franc CFA, marché
-- cible retenu) est une devise "zéro décimale" chez Stripe : sa plus petite unité EST l'unité
-- d'affichage (pas de sous-unité comme le centime), donc price_cents=4900 signifie 4 900 XOF,
-- envoyé tel quel à l'API Stripe (voir com.schoolsaas.billing.StripeGatewayImpl, pas de *100).
CREATE TABLE plans (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code           VARCHAR(32)  NOT NULL UNIQUE,
    name           VARCHAR(100) NOT NULL,
    price_cents    INTEGER      NOT NULL,
    currency       VARCHAR(3)   NOT NULL DEFAULT 'XOF',
    max_students   INTEGER,
    stripe_price_id VARCHAR(64),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Grille tarifaire indicative (cahier-des-charges.md §4.1) — montants à confirmer avec le
-- porteur de projet, voir docs/ARCHITECTURE.md ADR-009. stripe_price_id reste NULL tant que
-- les prix Stripe correspondants n'ont pas été créés côté dashboard (le plan reste alors
-- listable mais pas achetable en ligne, voir BillingCheckoutService).
INSERT INTO plans (code, name, price_cents, max_students) VALUES
    ('ESSENTIEL', 'Essentiel', 4900, 150),
    ('STANDARD', 'Standard', 14900, 800),
    ('PREMIUM', 'Premium / Enterprise', 0, NULL);
