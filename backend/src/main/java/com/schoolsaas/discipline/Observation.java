package com.schoolsaas.discipline;

import com.schoolsaas.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Filter;

/** Observation positive ou négative (cahier-des-charges.md §17), indépendante d'un incident. */
@Entity
@Table(name = "observations")
@Filter(name = "tenantFilter", condition = "school_id = :tenantId")
public class Observation extends TenantScopedEntity {

    @Column(name = "student_id", nullable = false, updatable = false)
    private Long studentId;

    @Column(nullable = false, updatable = false)
    private boolean positive;

    @Column(nullable = false)
    private String description;

    @Column(name = "author_user_id", nullable = false, updatable = false)
    private Long authorUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Observation() {
    }

    public Observation(Long studentId, boolean positive, String description, Long authorUserId) {
        this.studentId = studentId;
        this.positive = positive;
        this.description = description;
        this.authorUserId = authorUserId;
        this.createdAt = Instant.now();
    }

    public Long getStudentId() {
        return studentId;
    }

    public boolean isPositive() {
        return positive;
    }

    public String getDescription() {
        return description;
    }

    public Long getAuthorUserId() {
        return authorUserId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
