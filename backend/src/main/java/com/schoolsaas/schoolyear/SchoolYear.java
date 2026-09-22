package com.schoolsaas.schoolyear;

import com.schoolsaas.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.Filter;

/** Année scolaire (cahier-des-charges.md §7). */
@Entity
@Table(name = "school_years")
@Filter(name = "tenantFilter", condition = "school_id = :tenantId")
public class SchoolYear extends TenantScopedEntity {

    @Column(nullable = false)
    private String label;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SchoolYearStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected SchoolYear() {
    }

    public SchoolYear(String label, LocalDate startDate, LocalDate endDate, SchoolYearStatus status) {
        this.label = label;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.createdAt = Instant.now();
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public SchoolYearStatus getStatus() {
        return status;
    }

    public void setStatus(SchoolYearStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
