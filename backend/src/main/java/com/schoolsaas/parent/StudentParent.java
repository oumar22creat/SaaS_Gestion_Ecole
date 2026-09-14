package com.schoolsaas.parent;

import com.schoolsaas.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Filter;

/** Association élève/parent (cahier-des-charges.md §7). */
@Entity
@Table(name = "student_parents")
@Filter(name = "tenantFilter", condition = "school_id = :tenantId")
public class StudentParent extends TenantScopedEntity {

    @Column(name = "student_id", nullable = false, updatable = false)
    private Long studentId;

    @Column(name = "parent_id", nullable = false, updatable = false)
    private Long parentId;

    @Column(nullable = false)
    private String relationship;

    @Column(name = "primary_contact", nullable = false)
    private boolean primaryContact;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected StudentParent() {
    }

    public StudentParent(Long studentId, Long parentId, String relationship, boolean primaryContact) {
        this.studentId = studentId;
        this.parentId = parentId;
        this.relationship = relationship;
        this.primaryContact = primaryContact;
        this.createdAt = Instant.now();
    }

    public Long getStudentId() {
        return studentId;
    }

    public Long getParentId() {
        return parentId;
    }

    public String getRelationship() {
        return relationship;
    }

    public boolean isPrimaryContact() {
        return primaryContact;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
