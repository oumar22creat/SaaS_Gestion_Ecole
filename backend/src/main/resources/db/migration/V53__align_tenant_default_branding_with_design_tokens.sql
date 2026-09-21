-- Aligne le branding par défaut des établissements sur les design tokens du produit
-- (docs/DESIGN.md §2 : --tenant-primary #0f5c4c, --tenant-secondary #c9a227).
-- V50 avait retenu le bleu par défaut d'Ionic (#3880ff / #3dc2ff), ce qui donnait à tout
-- nouvel établissement une identité différente de celle des applications Web et Mobile.
ALTER TABLE tenants
    ALTER COLUMN primary_color SET DEFAULT '#0f5c4c',
    ALTER COLUMN secondary_color SET DEFAULT '#c9a227';

-- Rattrape uniquement les établissements restés sur l'ancien couple de valeurs par
-- défaut : un établissement qui a personnalisé ses couleurs n'est jamais écrasé.
UPDATE tenants
SET primary_color   = '#0f5c4c',
    secondary_color = '#c9a227'
WHERE primary_color = '#3880ff'
  AND secondary_color = '#3dc2ff';
