-- Portail parent/élève : rattache un compte de connexion (`users`) à une fiche métier.
-- Jusqu'ici, `students` et `parents` étaient des fiches isolées, sans lien vers un compte :
-- un parent connecté n'aurait eu aucun moyen de savoir quels élèves sont ses enfants.
-- Lève l'ADR-010 sur le volet "compte lié", pas sur le périmètre fonctionnel.

ALTER TABLE students
    ADD COLUMN user_id BIGINT REFERENCES users (id) ON DELETE SET NULL;

ALTER TABLE parents
    ADD COLUMN user_id BIGINT REFERENCES users (id) ON DELETE SET NULL;

-- Un compte ne peut être rattaché qu'à une seule fiche. L'unicité porte sur (school_id,
-- user_id) et non sur user_id seul : la contrainte reste alignée sur le découpage tenant du
-- reste du schéma, et l'index sert aussi la recherche "quelle fiche pour ce compte ?".
CREATE UNIQUE INDEX students_user_id_unique
    ON students (school_id, user_id)
    WHERE user_id IS NOT NULL;

CREATE UNIQUE INDEX parents_user_id_unique
    ON parents (school_id, user_id)
    WHERE user_id IS NOT NULL;

-- La colonne est volontairement NULLable : la très grande majorité des élèves n'aura jamais
-- de compte (écoles primaires notamment), et l'inscription d'un élève ne doit pas dépendre
-- de la création d'un accès.
COMMENT ON COLUMN students.user_id IS 'Compte de connexion de l''élève, NULL s''il n''en a pas';
COMMENT ON COLUMN parents.user_id IS 'Compte de connexion du parent, NULL s''il n''en a pas';
