package com.schoolsaas.schoolfees;

import com.schoolsaas.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.Filter;

/** Ligne de grille tarifaire (cahier-des-charges.md §19.4), rattachée à une classe. */
@Entity
@Table(name = "fee_schedules")
@Filter(name = "tenantFilter", condition = "school_id = :tenantId")
public class FeeSchedule extends TenantScopedEntity {

    @Column(name = "school_class_id", nullable = false, updatable = false)
    private Long schoolClassId;

    @Column(nullable = false)
    private String label;

    @Column(name = "amount_cents", nullable = false)
    private long amountCents;

    @Column(nullable = false)
    private String currency;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected FeeSchedule() {
    }

    public FeeSchedule(Long schoolClassId, String label, long amountCents, String currency, LocalDate dueDate) {
        this.schoolClassId = schoolClassId;
        this.label = label;
        this.amountCents = amountCents;
        this.currency = currency;
        this.dueDate = dueDate;
        this.createdAt = Instant.now();
    }

    public Long getSchoolClassId() {
        return schoolClassId;
    }

    public String getLabel() {
        return label;
    }

    public long getAmountCents() {
        return amountCents;
    }

    public String getCurrency() {
        return currency;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
