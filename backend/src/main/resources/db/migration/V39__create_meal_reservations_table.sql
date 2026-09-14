-- Réservation d'un repas pour un élève à une date donnée (cahier-des-charges.md §19.1).
-- Saisie par le personnel pour cette passe (pas de portail parent, voir ADR-010/ADR-025) :
-- reserved_by_user_id porte toujours un compte staff.
CREATE TABLE meal_reservations (
    id                  BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    school_id           BIGINT      NOT NULL REFERENCES tenants (id),
    student_id          BIGINT      NOT NULL REFERENCES students (id),
    date                DATE        NOT NULL,
    special_diet        BOOLEAN     NOT NULL DEFAULT false,
    reserved_by_user_id BIGINT      NOT NULL REFERENCES users (id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (student_id, date)
);

CREATE INDEX idx_meal_reservations_school_id ON meal_reservations (school_id);
CREATE INDEX idx_meal_reservations_student_date ON meal_reservations (student_id, date);

ALTER TABLE meal_reservations ENABLE ROW LEVEL SECURITY;
ALTER TABLE meal_reservations FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_policy ON meal_reservations
    USING (school_id = current_setting('app.tenant_id', true)::bigint);
