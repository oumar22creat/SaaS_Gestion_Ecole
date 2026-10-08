-- Bulletin au format officiel ouest-africain (maquette fournie par l'exploitant : Groupe
-- Scolaire PIA, Dakar).
--
-- Trois manques empêchaient de produire ce document.
--
-- 1. L'en-tête. Le bulletin est une pièce administrative : il porte l'État, l'académie et
--    l'inspection dont relève l'établissement, puis se signe. Un seul champ de texte libre
--    (report_card_header) ne permettait ni de les disposer, ni de les traduire d'un pays à
--    l'autre. Les libellés restent saisis en entier par l'école — « IA : PIKINE
--    GUEDIAWAYE » au Sénégal, « AE : BAMAKO RIVE DROITE » au Mali : le découpage
--    administratif n'est pas le même et le figer dans le code reviendrait à ne servir qu'un
--    seul pays.
-- 2. Le détail des notes. Le bulletin montre devoir 1, devoir 2 et composition côte à côte,
--    pas seulement leur moyenne. Les trois colonnes sont celles que les familles lisent :
--    une moyenne de 12 ne dit pas si l'élève a progressé entre le devoir et la composition.
-- 3. La distinction devoir / composition. Rien dans `exams` ne la portait, alors qu'elle
--    commande la colonne où la note se range et, dans ces systèmes, son poids.

ALTER TABLE tenants
    -- « REPUBLIQUE DU SENEGAL », « RÉPUBLIQUE DU MALI » — saisi tel qu'il doit s'imprimer.
    ADD COLUMN official_authority  VARCHAR(120),
    -- Académie ou direction régionale, ligne complète : « IA : PIKINE GUEDIAWAYE ».
    ADD COLUMN academy_label       VARCHAR(120),
    -- Inspection ou circonscription : « IEF : THIAROYE ».
    ADD COLUMN inspection_label    VARCHAR(120),
    -- Signataire du bulletin et du certificat. Sans lui, le document n'est pas opposable.
    ADD COLUMN director_name       VARCHAR(120),
    -- Ville de signature : « Dakar, le 30/03/2021 ».
    ADD COLUMN head_office_city    VARCHAR(120),
    ADD COLUMN postal_address      VARCHAR(255);

-- DEVOIR par défaut : c'est le cas courant, et requalifier les compositions existantes au
-- jugé serait pire qu'un défaut assumé que l'école corrige en deux clics.
ALTER TABLE exams
    ADD COLUMN exam_type VARCHAR(16) NOT NULL DEFAULT 'DEVOIR';

-- Notes figées à la génération, comme la moyenne et le coefficient le sont déjà : un
-- bulletin réédité six mois plus tard doit rendre le même document, même si une note a été
-- corrigée depuis.
ALTER TABLE report_card_entries
    ADD COLUMN assignment_one_score DOUBLE PRECISION,
    ADD COLUMN assignment_two_score DOUBLE PRECISION,
    ADD COLUMN exam_score           DOUBLE PRECISION;

-- Sert au tri « quelle est la 1re, la 2e interrogation » de chaque matière sur la période.
CREATE INDEX idx_exams_class_subject_date ON exams (school_class_id, subject_id, exam_date);
