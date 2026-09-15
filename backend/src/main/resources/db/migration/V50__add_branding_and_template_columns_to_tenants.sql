-- Personnalisation par établissement (cahier-des-charges.md §2.4/§2.3, ROADMAP.md 3.7).
-- Valeurs par défaut = branding neutre déjà utilisé côté Web/Mobile en fallback (voir
-- web/src/app/branding/tenant-branding.model.ts DEFAULT_BRANDING) : un tenant qui n'a jamais
-- configuré son branding obtient exactement le même résultat visuel qu'avant cette migration.
ALTER TABLE tenants
    ADD COLUMN logo_url                  VARCHAR(500),
    ADD COLUMN primary_color             VARCHAR(7) NOT NULL DEFAULT '#3880ff',
    ADD COLUMN secondary_color           VARCHAR(7) NOT NULL DEFAULT '#3dc2ff',
    ADD COLUMN custom_domain             VARCHAR(255) UNIQUE,
    ADD COLUMN report_card_header        VARCHAR(255),
    ADD COLUMN report_card_legal_mentions VARCHAR(2000);
