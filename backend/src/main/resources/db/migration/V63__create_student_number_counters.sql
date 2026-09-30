-- Compteur de matricules, par établissement et par année.
--
-- Pourquoi une table plutôt qu'un MAX(student_number) + 1 : deux secrétaires inscrivant un
-- élève au même instant liraient le même maximum et produiraient le même matricule. L'une des
-- deux insertions échouerait sur la contrainte d'unicité — au mieux. Ici l'incrément est fait
-- par PostgreSQL dans une seule instruction (INSERT ... ON CONFLICT DO UPDATE ... RETURNING),
-- donc sérialisé par la base elle-même.
--
-- Une ligne par année : le matricule porte l'année d'inscription (2026-0001), ce qui situe la
-- promotion d'un coup d'œil et garde le numéro court puisqu'il repart à 1 chaque année.
CREATE TABLE student_number_counters (
    school_id  BIGINT NOT NULL REFERENCES tenants (id),
    year       INT    NOT NULL,
    last_value INT    NOT NULL,
    PRIMARY KEY (school_id, year)
);

ALTER TABLE student_number_counters ENABLE ROW LEVEL SECURITY;
ALTER TABLE student_number_counters FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_policy ON student_number_counters
    USING (school_id = current_setting('app.tenant_id', true)::bigint);
