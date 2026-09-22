-- Année scolaire et inscriptions (cahier-des-charges.md §7).
--
-- Jusqu'ici le produit n'avait aucune notion d'année : la classe d'un élève tenait dans une
-- seule colonne `students.school_class_id`, et la période d'un bulletin dans du texte libre.
-- Conséquences concrètes : impossible de clôturer une année, de faire passer une promotion en
-- classe supérieure, ou de retrouver dans quelle classe était un élève l'an dernier — changer
-- sa classe écrasait son passé.
--
-- `students.school_class_id` est conservée : elle devient la classe COURANTE, dénormalisée
-- depuis l'inscription de l'année active. Tout le produit (appel, notes, frais, emploi du
-- temps) continue de la lire sans modification ; seules les opérations d'inscription et de
-- passage écrivent les deux. L'historique complet vit dans `enrollments`.

CREATE TABLE school_years (
    id         BIGSERIAL PRIMARY KEY,
    school_id  BIGINT      NOT NULL REFERENCES tenants (id),
    label      VARCHAR(50) NOT NULL,
    start_date DATE        NOT NULL,
    end_date   DATE        NOT NULL,

    -- PLANNED : préparée, on peut y inscrire sans perturber l'année en cours.
    -- ACTIVE   : celle sur laquelle travaille tout le produit.
    -- CLOSED   : archivée, lecture seule.
    status     VARCHAR(16) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT school_years_dates_ordered CHECK (end_date > start_date)
);

CREATE UNIQUE INDEX school_years_label_idx ON school_years (school_id, label);

-- Une seule année active par établissement : sans cette contrainte, « l'année en cours »
-- deviendrait ambigu et chaque écran choisirait la sienne.
CREATE UNIQUE INDEX school_years_single_active_idx
    ON school_years (school_id) WHERE status = 'ACTIVE';

ALTER TABLE school_years ENABLE ROW LEVEL SECURITY;
ALTER TABLE school_years FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON school_years
    USING (school_id = current_setting('app.tenant_id', true)::bigint);


CREATE TABLE enrollments (
    id              BIGSERIAL PRIMARY KEY,
    school_id       BIGINT      NOT NULL REFERENCES tenants (id),
    school_year_id  BIGINT      NOT NULL REFERENCES school_years (id),
    student_id      BIGINT      NOT NULL REFERENCES students (id),
    school_class_id BIGINT      NOT NULL REFERENCES school_classes (id),

    -- ENROLLED    : inscrit, année en cours.
    -- PROMOTED    : a terminé l'année et passe dans la classe supérieure.
    -- REPEATING   : redouble — reste sur la même classe l'année suivante.
    -- TRANSFERRED : parti dans un autre établissement.
    -- GRADUATED   : a terminé le cycle.
    -- WITHDRAWN   : a quitté l'établissement en cours d'année.
    status          VARCHAR(16) NOT NULL,

    enrolled_at     DATE        NOT NULL,
    left_at         DATE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Un élève n'est inscrit qu'une fois par année : c'est ce qui rend le passage de classe
-- idempotent, et ce qui empêche une double inscription lors d'une rentrée relancée.
CREATE UNIQUE INDEX enrollments_student_year_idx
    ON enrollments (school_id, school_year_id, student_id);

CREATE INDEX enrollments_year_class_idx ON enrollments (school_id, school_year_id, school_class_id);
CREATE INDEX enrollments_student_idx ON enrollments (school_id, student_id);

ALTER TABLE enrollments ENABLE ROW LEVEL SECURITY;
ALTER TABLE enrollments FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON enrollments
    USING (school_id = current_setting('app.tenant_id', true)::bigint);


-- Reprise de l'existant : chaque établissement ayant déjà des élèves reçoit une année active
-- couvrant la rentrée en cours, et chaque élève déjà affecté à une classe y est inscrit.
-- Sans cette reprise, les écrans s'ouvriraient sur « aucune année scolaire » alors que
-- l'établissement travaille depuis des mois.
--
-- Découpage retenu : rentrée en octobre, fin en juillet (calendrier scolaire malien). Une
-- école qui suit un autre calendrier corrige les dates depuis l'écran Années scolaires.
INSERT INTO school_years (school_id, label, start_date, end_date, status)
SELECT DISTINCT
    s.school_id,
    CASE WHEN EXTRACT(MONTH FROM CURRENT_DATE) >= 8
         THEN EXTRACT(YEAR FROM CURRENT_DATE) || '-' || (EXTRACT(YEAR FROM CURRENT_DATE) + 1)
         ELSE (EXTRACT(YEAR FROM CURRENT_DATE) - 1) || '-' || EXTRACT(YEAR FROM CURRENT_DATE)
    END,
    CASE WHEN EXTRACT(MONTH FROM CURRENT_DATE) >= 8
         THEN make_date(EXTRACT(YEAR FROM CURRENT_DATE)::int, 10, 1)
         ELSE make_date((EXTRACT(YEAR FROM CURRENT_DATE) - 1)::int, 10, 1)
    END,
    CASE WHEN EXTRACT(MONTH FROM CURRENT_DATE) >= 8
         THEN make_date((EXTRACT(YEAR FROM CURRENT_DATE) + 1)::int, 7, 31)
         ELSE make_date(EXTRACT(YEAR FROM CURRENT_DATE)::int, 7, 31)
    END,
    'ACTIVE'
FROM students s;

INSERT INTO enrollments (school_id, school_year_id, student_id, school_class_id, status, enrolled_at)
SELECT s.school_id, y.id, s.id, s.school_class_id, 'ENROLLED', y.start_date
FROM students s
JOIN school_years y ON y.school_id = s.school_id AND y.status = 'ACTIVE'
WHERE s.school_class_id IS NOT NULL;
