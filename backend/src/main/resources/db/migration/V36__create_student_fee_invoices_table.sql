-- Facture de frais de scolarité par élève, générée en masse à partir d'une grille tarifaire
-- (cahier-des-charges.md §19.4). status recalculé automatiquement selon les paiements reçus
-- (fee_payments) : voir SchoolFeesService.
CREATE TABLE student_fee_invoices (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    school_id        BIGINT      NOT NULL REFERENCES tenants (id),
    fee_schedule_id  BIGINT      NOT NULL REFERENCES fee_schedules (id),
    student_id       BIGINT      NOT NULL REFERENCES students (id),
    amount_due_cents BIGINT      NOT NULL,
    status           VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    issued_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (fee_schedule_id, student_id)
);

CREATE INDEX idx_student_fee_invoices_school_id ON student_fee_invoices (school_id);
CREATE INDEX idx_student_fee_invoices_student_id ON student_fee_invoices (student_id);
CREATE INDEX idx_student_fee_invoices_schedule_id ON student_fee_invoices (fee_schedule_id);

ALTER TABLE student_fee_invoices ENABLE ROW LEVEL SECURITY;
ALTER TABLE student_fee_invoices FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_policy ON student_fee_invoices
    USING (school_id = current_setting('app.tenant_id', true)::bigint);
