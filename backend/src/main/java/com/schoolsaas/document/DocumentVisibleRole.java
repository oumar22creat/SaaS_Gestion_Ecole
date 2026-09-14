package com.schoolsaas.document;

import com.schoolsaas.auth.Role;
import com.schoolsaas.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;

/** Rôle autorisé à consulter un document — aucune ligne = visible par tous les rôles staff. */
@Entity
@Table(name = "document_visible_roles")
@Filter(name = "tenantFilter", condition = "school_id = :tenantId")
public class DocumentVisibleRole extends TenantScopedEntity {

    @Column(name = "document_id", nullable = false, updatable = false)
    private Long documentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private Role role;

    protected DocumentVisibleRole() {
    }

    public DocumentVisibleRole(Long documentId, Role role) {
        this.documentId = documentId;
        this.role = role;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public Role getRole() {
        return role;
    }
}
