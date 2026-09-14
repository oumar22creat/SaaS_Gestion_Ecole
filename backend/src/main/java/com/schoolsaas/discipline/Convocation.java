package com.schoolsaas.discipline;

import com.schoolsaas.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Filter;

/** Convocation d'un élève et/ou de son parent (cahier-des-charges.md §17). */
@Entity
@Table(name = "convocations")
@Filter(name = "tenantFilter", condition = "school_id = :tenantId")
public class Convocation extends TenantScopedEntity {

    @Column(name = "student_id", nullable = false, updatable = false)
    private Long studentId;

    @Column(name = "convene_parent", nullable = false, updatable = false)
    private boolean convokeParent;

    @Column(nullable = false)
    private String reason;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConvocationStatus status;

    @Column(name = "created_by_user_id", nullable = false, updatable = false)
    private Long createdByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Convocation() {
    }

    public Convocation(Long studentId, boolean convokeParent, String reason, Instant scheduledAt, Long createdByUserId) {
        this.studentId = studentId;
        this.convokeParent = convokeParent;
        this.reason = reason;
        this.scheduledAt = scheduledAt;
        this.status = ConvocationStatus.SCHEDULED;
        this.createdByUserId = createdByUserId;
        this.createdAt = Instant.now();
    }

    public Long getStudentId() {
        return studentId;
    }

    public boolean isConvokeParent() {
        return convokeParent;
    }

    public String getReason() {
        return reason;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public ConvocationStatus getStatus() {
        return status;
    }

    public void setStatus(ConvocationStatus status) {
        this.status = status;
    }

    public Long getCreatedByUserId() {
        return createdByUserId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
