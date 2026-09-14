-- Emprunts et retours (cahier-des-charges.md §19.3). returned_at NULL = emprunt actif ; en
-- retard si due_date < aujourd'hui et returned_at NULL (calculé à la lecture, pas un statut
-- stocké séparément — voir LibraryService).
CREATE TABLE book_loans (
    id           BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    school_id    BIGINT      NOT NULL REFERENCES tenants (id),
    book_id      BIGINT      NOT NULL REFERENCES books (id),
    student_id   BIGINT      NOT NULL REFERENCES students (id),
    borrowed_at  DATE        NOT NULL,
    due_date     DATE        NOT NULL,
    returned_at  TIMESTAMPTZ
);

CREATE INDEX idx_book_loans_school_id ON book_loans (school_id);
CREATE INDEX idx_book_loans_book_id ON book_loans (book_id);
CREATE INDEX idx_book_loans_student_id ON book_loans (student_id);
CREATE INDEX idx_book_loans_due_date ON book_loans (due_date) WHERE returned_at IS NULL;

ALTER TABLE book_loans ENABLE ROW LEVEL SECURITY;
ALTER TABLE book_loans FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_policy ON book_loans
    USING (school_id = current_setting('app.tenant_id', true)::bigint);
