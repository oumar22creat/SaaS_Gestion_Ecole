-- Droits de consultation par rôle (cahier-des-charges.md §14) : aucune ligne = visible par
-- tous les rôles staff du tenant (simplification MVP, voir docs/ARCHITECTURE.md ADR-017).
CREATE TABLE document_visible_roles (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    school_id   BIGINT      NOT NULL REFERENCES tenants (id),
    document_id BIGINT      NOT NULL REFERENCES documents (id),
    role        VARCHAR(32) NOT NULL,
    UNIQUE (document_id, role)
);

CREATE INDEX idx_document_visible_roles_document_id ON document_visible_roles (document_id);

ALTER TABLE document_visible_roles ENABLE ROW LEVEL SECURITY;
ALTER TABLE document_visible_roles FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_policy ON document_visible_roles
    USING (school_id = current_setting('app.tenant_id', true)::bigint);
