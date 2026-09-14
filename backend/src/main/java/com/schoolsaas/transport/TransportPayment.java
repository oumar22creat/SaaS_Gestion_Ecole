package com.schoolsaas.transport;

import com.schoolsaas.common.TenantScopedEntity;
import com.schoolsaas.schoolfees.FeePaymentMethod;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Filter;

/** Paiement (partiel ou total) d'une {@link TransportInvoice}, saisi manuellement par le personnel. */
@Entity
@Table(name = "transport_payments")
@Filter(name = "tenantFilter", condition = "school_id = :tenantId")
public class TransportPayment extends TenantScopedEntity {

    @Column(name = "invoice_id", nullable = false, updatable = false)
    private Long invoiceId;

    @Column(name = "amount_cents", nullable = false, updatable = false)
    private long amountCents;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private FeePaymentMethod method;

    @Column
    private String reference;

    @Column(name = "recorded_by_user_id", nullable = false, updatable = false)
    private Long recordedByUserId;

    @Column(name = "paid_at", nullable = false, updatable = false)
    private Instant paidAt;

    protected TransportPayment() {
    }

    public TransportPayment(Long invoiceId, long amountCents, FeePaymentMethod method, String reference, Long recordedByUserId) {
        this.invoiceId = invoiceId;
        this.amountCents = amountCents;
        this.method = method;
        this.reference = reference;
        this.recordedByUserId = recordedByUserId;
        this.paidAt = Instant.now();
    }

    public Long getInvoiceId() {
        return invoiceId;
    }

    public long getAmountCents() {
        return amountCents;
    }

    public FeePaymentMethod getMethod() {
        return method;
    }

    public String getReference() {
        return reference;
    }

    public Long getRecordedByUserId() {
        return recordedByUserId;
    }

    public Instant getPaidAt() {
        return paidAt;
    }
}
