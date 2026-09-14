package com.schoolsaas.discipline;

import com.schoolsaas.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Filter;

/** Sanction ou punition (cahier-des-charges.md §17), toujours rattachée à un {@link Incident} et à un élève précis. */
@Entity
@Table(name = "sanctions")
@Filter(name = "tenantFilter", condition = "school_id = :tenantId")
public class Sanction extends TenantScopedEntity {

    @Column(name = "incident_id", nullable = false, updatable = false)
    private Long incidentId;

    @Column(name = "student_id", nullable = false, updatable = false)
    private Long studentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private SanctionType type;

    @Column(name = "duration_days")
    private Integer durationDays;

    @Column
    private String description;

    @Column(name = "decided_by_user_id", nullable = false, updatable = false)
    private Long decidedByUserId;

    @Column(name = "decided_at", nullable = false, updatable = false)
    private Instant decidedAt;

    protected Sanction() {
    }

    public Sanction(
            Long incidentId, Long studentId, SanctionType type, Integer durationDays, String description, Long decidedByUserId) {
        this.incidentId = incidentId;
        this.studentId = studentId;
        this.type = type;
        this.durationDays = durationDays;
        this.description = description;
        this.decidedByUserId = decidedByUserId;
        this.decidedAt = Instant.now();
    }

    public Long getIncidentId() {
        return incidentId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public SanctionType getType() {
        return type;
    }

    public Integer getDurationDays() {
        return durationDays;
    }

    public String getDescription() {
        return description;
    }

    public Long getDecidedByUserId() {
        return decidedByUserId;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }
}
