package com.schoolsaas.schoolfees;

import com.schoolsaas.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Filter;

/** Facture d'un élève pour une ligne de grille tarifaire (cahier-des-charges.md §19.4). */
@Entity
@Table(name = "student_fee_invoices")
@Filter(name = "tenantFilter", condition = "school_id = :tenantId")
public class StudentFeeInvoice extends TenantScopedEntity {

    @Column(name = "fee_schedule_id", nullable = false, updatable = false)
    private Long feeScheduleId;

    @Column(name = "student_id", nullable = false, updatable = false)
    private Long studentId;

    @Column(name = "amount_due_cents", nullable = false, updatable = false)
    private long amountDueCents;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeeInvoiceStatus status;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    protected StudentFeeInvoice() {
    }

    public StudentFeeInvoice(Long feeScheduleId, Long studentId, long amountDueCents) {
        this.feeScheduleId = feeScheduleId;
        this.studentId = studentId;
        this.amountDueCents = amountDueCents;
        this.status = FeeInvoiceStatus.PENDING;
        this.issuedAt = Instant.now();
    }

    public Long getFeeScheduleId() {
        return feeScheduleId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public long getAmountDueCents() {
        return amountDueCents;
    }

    public FeeInvoiceStatus getStatus() {
        return status;
    }

    public void setStatus(FeeInvoiceStatus status) {
        this.status = status;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }
}
