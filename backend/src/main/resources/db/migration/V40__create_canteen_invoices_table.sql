-- Facturation liée à la consommation réelle (cahier-des-charges.md §19.1) : générée à la
-- demande à partir du nombre de repas réservés sur une période, pas d'abonnement forfaitaire.
CREATE TABLE canteen_invoices (
    id                     BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    school_id              BIGINT      NOT NULL REFERENCES tenants (id),
    student_id             BIGINT      NOT NULL REFERENCES students (id),
    period_from            DATE        NOT NULL,
    period_to              DATE        NOT NULL,
    meal_count             INTEGER     NOT NULL,
    price_per_meal_cents   BIGINT      NOT NULL,
    amount_due_cents       BIGINT      NOT NULL,
    status                 VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    issued_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_canteen_invoices_school_id ON canteen_invoices (school_id);
CREATE INDEX idx_canteen_invoices_student_id ON canteen_invoices (student_id);

ALTER TABLE canteen_invoices ENABLE ROW LEVEL SECURITY;
ALTER TABLE canteen_invoices FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_policy ON canteen_invoices
    USING (school_id = current_setting('app.tenant_id', true)::bigint);
