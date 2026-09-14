-- Cahier de textes et devoirs (cahier-des-charges.md §13) : contenu du cours et travail à
-- faire regroupés dans une même séance, comme un vrai cahier de textes (pas deux entités
-- séparées) — voir docs/ARCHITECTURE.md ADR-018.
CREATE TABLE lessons (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    school_id             BIGINT       NOT NULL REFERENCES tenants (id),
    school_class_id       BIGINT       NOT NULL REFERENCES school_classes (id),
    subject_id            BIGINT       NOT NULL REFERENCES subjects (id),
    session_date          DATE         NOT NULL,
    content               VARCHAR(4000) NOT NULL,
    homework              VARCHAR(2000),
    homework_due_date     DATE,
    attachment_document_id BIGINT REFERENCES documents (id),
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_lessons_school_id ON lessons (school_id);
CREATE INDEX idx_lessons_class_date ON lessons (school_class_id, session_date);

ALTER TABLE lessons ENABLE ROW LEVEL SECURITY;
ALTER TABLE lessons FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_policy ON lessons
    USING (school_id = current_setting('app.tenant_id', true)::bigint);
