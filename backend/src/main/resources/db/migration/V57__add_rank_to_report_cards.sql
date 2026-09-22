-- Rang de l'élève dans sa classe (cahier-des-charges.md §12).
--
-- Le rang manquait alors qu'au Mali c'est la première ligne que lit un parent sur un
-- bulletin, avant même la moyenne. Il est stocké et non calculé à la lecture : un bulletin
-- est un document remis aux familles à une date donnée, son rang doit rester celui du
-- conseil de classe même si une note est corrigée après coup. Régénérer le bulletin
-- recalcule les deux ensemble.
--
-- `class_size` accompagne le rang : « 3e » ne veut rien dire sans « sur 42 ».
ALTER TABLE report_cards ADD COLUMN rank_in_class INTEGER;
ALTER TABLE report_cards ADD COLUMN class_size INTEGER;
