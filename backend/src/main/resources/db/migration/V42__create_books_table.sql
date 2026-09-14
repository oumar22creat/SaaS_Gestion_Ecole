-- Catalogue des ouvrages (cahier-des-charges.md §19.3). barcode est la clé de scan
-- opérationnelle (toujours présente) ; isbn reste optionnel (anciens ouvrages, matériel
-- interne sans ISBN).
CREATE TABLE books (
    id              BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    school_id       BIGINT       NOT NULL REFERENCES tenants (id),
    barcode         VARCHAR(64)  NOT NULL,
    isbn            VARCHAR(20),
    title           VARCHAR(255) NOT NULL,
    author          VARCHAR(255) NOT NULL,
    total_copies    INTEGER      NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (school_id, barcode)
);

CREATE INDEX idx_books_school_id ON books (school_id);

ALTER TABLE books ENABLE ROW LEVEL SECURITY;
ALTER TABLE books FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_policy ON books
    USING (school_id = current_setting('app.tenant_id', true)::bigint);
