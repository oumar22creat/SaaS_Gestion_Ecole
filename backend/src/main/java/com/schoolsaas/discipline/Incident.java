package com.schoolsaas.discipline;

import com.schoolsaas.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.Filter;

/** Incident disciplinaire (cahier-des-charges.md §17), rattaché à une classe. */
@Entity
@Table(name = "incidents")
@Filter(name = "tenantFilter", condition = "school_id = :tenantId")
public class Incident extends TenantScopedEntity {

    @Column(name = "school_class_id", nullable = false, updatable = false)
    private Long schoolClassId;

    @Column(name = "occurred_at", nullable = false)
    private LocalDate occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentSeverity severity;

    @Column(nullable = false)
    private String description;

    @Column(name = "reported_by_user_id", nullable = false, updatable = false)
    private Long reportedByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Incident() {
    }

    public Incident(Long schoolClassId, LocalDate occurredAt, IncidentSeverity severity, String description, Long reportedByUserId) {
        this.schoolClassId = schoolClassId;
        this.occurredAt = occurredAt;
        this.severity = severity;
        this.description = description;
        this.reportedByUserId = reportedByUserId;
        this.createdAt = Instant.now();
    }

    public Long getSchoolClassId() {
        return schoolClassId;
    }

    public LocalDate getOccurredAt() {
        return occurredAt;
    }

    public IncidentSeverity getSeverity() {
        return severity;
    }

    public String getDescription() {
        return description;
    }

    public Long getReportedByUserId() {
        return reportedByUserId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
