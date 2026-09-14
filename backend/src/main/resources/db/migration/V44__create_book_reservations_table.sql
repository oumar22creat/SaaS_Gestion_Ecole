-- Réservations et liste d'attente (cahier-des-charges.md §19.3). La position dans la liste
-- d'attente est l'ordre de reserved_at, pas une colonne stockée (voir LibraryService).
CREATE TABLE book_reservations (
    id            BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    school_id     BIGINT      NOT NULL REFERENCES tenants (id),
    book_id       BIGINT      NOT NULL REFERENCES books (id),
    student_id    BIGINT      NOT NULL REFERENCES students (id),
    status        VARCHAR(16) NOT NULL DEFAULT 'WAITING',
    reserved_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_book_reservations_school_id ON book_reservations (school_id);
CREATE INDEX idx_book_reservations_book_id ON book_reservations (book_id);

ALTER TABLE book_reservations ENABLE ROW LEVEL SECURITY;
ALTER TABLE book_reservations FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_policy ON book_reservations
    USING (school_id = current_setting('app.tenant_id', true)::bigint);
