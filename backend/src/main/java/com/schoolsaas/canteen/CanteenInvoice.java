package com.schoolsaas.canteen;

import com.schoolsaas.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.Filter;

/** Facturation liée à la consommation réelle (cahier-des-charges.md §19.1), sur une période donnée. */
@Entity
@Table(name = "canteen_invoices")
@Filter(name = "tenantFilter", condition = "school_id = :tenantId")
public class CanteenInvoice extends TenantScopedEntity {

    @Column(name = "student_id", nullable = false, updatable = false)
    private Long studentId;

    @Column(name = "period_from", nullable = false, updatable = false)
    private LocalDate periodFrom;

    @Column(name = "period_to", nullable = false, updatable = false)
    private LocalDate periodTo;

    @Column(name = "meal_count", nullable = false, updatable = false)
    private int mealCount;

    @Column(name = "price_per_meal_cents", nullable = false, updatable = false)
    private long pricePerMealCents;

    @Column(name = "amount_due_cents", nullable = false, updatable = false)
    private long amountDueCents;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CanteenInvoiceStatus status;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    protected CanteenInvoice() {
    }

    public CanteenInvoice(
            Long studentId, LocalDate periodFrom, LocalDate periodTo, int mealCount, long pricePerMealCents) {
        this.studentId = studentId;
        this.periodFrom = periodFrom;
        this.periodTo = periodTo;
        this.mealCount = mealCount;
        this.pricePerMealCents = pricePerMealCents;
        this.amountDueCents = mealCount * pricePerMealCents;
        this.status = CanteenInvoiceStatus.PENDING;
        this.issuedAt = Instant.now();
    }

    public Long getStudentId() {
        return studentId;
    }

    public LocalDate getPeriodFrom() {
        return periodFrom;
    }

    public LocalDate getPeriodTo() {
        return periodTo;
    }

    public int getMealCount() {
        return mealCount;
    }

    public long getPricePerMealCents() {
        return pricePerMealCents;
    }

    public long getAmountDueCents() {
        return amountDueCents;
    }

    public CanteenInvoiceStatus getStatus() {
        return status;
    }

    public void setStatus(CanteenInvoiceStatus status) {
        this.status = status;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }
}
