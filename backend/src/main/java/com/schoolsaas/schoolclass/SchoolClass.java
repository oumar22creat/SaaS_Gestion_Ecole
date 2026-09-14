package com.schoolsaas.schoolclass;

import com.schoolsaas.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Filter;

/** Classe scolaire (cahier-des-charges.md §8). Nom de package : voir ADR-007 (`class` réservé Java). */
@Entity
@Table(name = "school_classes")
@Filter(name = "tenantFilter", condition = "school_id = :tenantId")
public class SchoolClass extends TenantScopedEntity {

    @Column(nullable = false)
    private String name;

    @Column(name = "head_teacher_id")
    private Long headTeacherId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected SchoolClass() {
    }

    public SchoolClass(String name, Long headTeacherId) {
        this.name = name;
        this.headTeacherId = headTeacherId;
        this.createdAt = Instant.now();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getHeadTeacherId() {
        return headTeacherId;
    }

    public void setHeadTeacherId(Long headTeacherId) {
        this.headTeacherId = headTeacherId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
