-- Enseignants (fiche RH minimale, cahier-des-charges.md §8/§26) — table métier scopée
-- school_id (CLAUDE.md règle 1). Volontairement pas de lien vers `users` : la fiche
-- enseignant (utilisée pour l'affectation classe/matière et l'emploi du temps) est
-- découplée de l'éventuel compte de connexion (Role.TEACHER) — ce compte n'existe pas encore
-- pour ce rôle en pratique (voir docs/ARCHITECTURE.md ADR-010).
CREATE TABLE teachers (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    school_id  BIGINT       NOT NULL REFERENCES tenants (id),
    first_name VARCHAR(100) NOT NULL,
    last_name  VARCHAR(100) NOT NULL,
    email      VARCHAR(255),
    phone      VARCHAR(32),
    active     BOOLEAN      NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_teachers_school_id ON teachers (school_id);

ALTER TABLE teachers ENABLE ROW LEVEL SECURITY;
ALTER TABLE teachers FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_policy ON teachers
    USING (school_id = current_setting('app.tenant_id', true)::bigint);
