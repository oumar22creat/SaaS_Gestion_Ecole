-- Grille tarifaire publique (site vitrine, section Tarifs).
--
-- V4 posait des montants indicatifs en attendant confirmation du porteur de projet
-- (docs/ARCHITECTURE.md ADR-009). Ils le sont désormais, et l'écart était important :
-- 4 900 contre 15 000 FCFA par mois sur le plan d'entrée. Un établissement voyait un prix
-- dans l'application et un autre sur le site, ce qui suffit à faire douter du reste.
--
-- price_cents reste MENSUEL : c'est ce que consomme le calcul du revenu récurrent
-- (PlatformDashboardService). Le site annonce le tarif annuel, qui vaut exactement douze fois
-- le mensuel pour les trois plans — l'application peut donc afficher les deux sans stocker
-- une seconde colonne qui pourrait diverger.
--
--   Essentiel   15 000 /mois → 180 000 /an     jusqu'à 150 élèves
--   Standard    62 500 /mois → 750 000 /an     jusqu'à 800 élèves
--   Premium    150 000 /mois → 1 800 000 /an   effectif illimité, tarif dégressif à négocier

UPDATE plans SET price_cents = 15000,  max_students = 150  WHERE code = 'ESSENTIEL';
UPDATE plans SET price_cents = 62500,  max_students = 800  WHERE code = 'STANDARD';

-- « dès 1 800 000 » : le prix affiché est un plancher, le tarif réel se négocie selon
-- l'effectif. Stocker le plancher vaut mieux que zéro, qui faisait compter ce plan pour rien
-- dans le revenu récurrent de la console plateforme.
UPDATE plans SET price_cents = 150000, max_students = NULL, name = 'Premium' WHERE code = 'PREMIUM';
