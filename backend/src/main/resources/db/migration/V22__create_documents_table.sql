-- Bibliothèque de documents (cahier-des-charges.md §14). `storage_key` est une clé opaque
-- résolue par StorageGateway (disque local en dev/MVP, S3 réel non câblé — voir
-- docs/ARCHITECTURE.md ADR-017) : jamais un chemin de fichier exposé directement à l'API.
CREATE TABLE documents (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    school_id       BIGINT       NOT NULL REFERENCES tenants (id),
    title           VARCHAR(255) NOT NULL,
    scope           VARCHAR(16)  NOT NULL,
    subject_id      BIGINT REFERENCES subjects (id),
    school_class_id BIGINT REFERENCES school_classes (id),
    service_label   VARCHAR(100),
    file_name       VARCHAR(255) NOT NULL,
    content_type    VARCHAR(100) NOT NULL,
    size_bytes      BIGINT       NOT NULL,
    storage_key      VARCHAR(255) NOT NULL,
    archived        BOOLEAN      NOT NULL DEFAULT false,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CHECK (
        (scope = 'SUBJECT' AND subject_id IS NOT NULL)
        OR (scope = 'CLASS' AND school_class_id IS NOT NULL)
        OR (scope = 'SERVICE' AND service_label IS NOT NULL)
    )
);

CREATE INDEX idx_documents_school_id ON documents (school_id);
CREATE INDEX idx_documents_subject_id ON documents (subject_id);
CREATE INDEX idx_documents_school_class_id ON documents (school_class_id);

ALTER TABLE documents ENABLE ROW LEVEL SECURITY;
ALTER TABLE documents FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_policy ON documents
    USING (school_id = current_setting('app.tenant_id', true)::bigint);
