-- Fonctionnalités activées par plan (cahier-des-charges.md §4.1/§2.5, ROADMAP.md 3.7).
-- Simplification assumée : "en option" (grille §4.1, plan Standard) est traité comme
-- équivalent à "inclus" — aucun mécanisme d'achat d'option ni de dérogation par tenant
-- (feature flag individuel, cahier §2.5) n'existe encore ; voir docs/ARCHITECTURE.md ADR-028.
ALTER TABLE plans
    ADD COLUMN canteen_included        BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN transport_included      BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN library_included        BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN custom_domain_included  BOOLEAN NOT NULL DEFAULT false;

UPDATE plans SET canteen_included = true, transport_included = true, library_included = true
    WHERE code IN ('STANDARD', 'PREMIUM');
UPDATE plans SET custom_domain_included = true WHERE code = 'PREMIUM';
