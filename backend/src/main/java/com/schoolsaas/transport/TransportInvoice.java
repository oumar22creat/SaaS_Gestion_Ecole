package com.schoolsaas.transport;

import com.schoolsaas.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.Filter;

/** Facturation du service de transport (cahier-des-charges.md §19.2), sur une période donnée. */
@Entity
@Table(name = "transport_invoices")
@Filter(name = "tenantFilter", condition = "school_id = :tenantId")
public class TransportInvoice extends TenantScopedEntity {

    @Column(name = "student_id", nullable = false, updatable = false)
    private Long studentId;

    @Column(name = "period_from", nullable = false, updatable = false)
    private LocalDate periodFrom;

    @Column(name = "period_to", nullable = false, updatable = false)
    private LocalDate periodTo;

    @Column(name = "amount_due_cents", nullable = false, updatable = false)
    private long amountDueCents;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransportInvoiceStatus status;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    protected TransportInvoice() {
    }

    public TransportInvoice(Long studentId, LocalDate periodFrom, LocalDate periodTo, long amountDueCents) {
        this.studentId = studentId;
        this.periodFrom = periodFrom;
        this.periodTo = periodTo;
        this.amountDueCents = amountDueCents;
        this.status = TransportInvoiceStatus.PENDING;
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

    public long getAmountDueCents() {
        return amountDueCents;
    }

    public TransportInvoiceStatus getStatus() {
        return status;
    }

    public void setStatus(TransportInvoiceStatus status) {
        this.status = status;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }
}
