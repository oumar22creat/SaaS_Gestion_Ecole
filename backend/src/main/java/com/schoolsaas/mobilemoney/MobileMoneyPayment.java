package com.schoolsaas.mobilemoney;

import com.schoolsaas.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Filter;

/** Tentative de paiement mobile money rattachée à une facture de frais de scolarité. */
@Entity
@Table(name = "mobile_money_payments")
@Filter(name = "tenantFilter", condition = "school_id = :tenantId")
public class MobileMoneyPayment extends TenantScopedEntity {

    @Column(name = "invoice_id", nullable = false, updatable = false)
    private Long invoiceId;

    @Column(name = "amount_cents", nullable = false, updatable = false)
    private long amountCents;

    @Column(name = "payer_msisdn", nullable = false, updatable = false)
    private String payerMsisdn;

    @Column(nullable = false, updatable = false)
    private String provider;

    @Column(nullable = false, updatable = false)
    private String reference;

    @Column(name = "provider_reference")
    private String providerReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MobileMoneyStatus status;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MobileMoneyPayment() {
    }

    public MobileMoneyPayment(
            Long invoiceId, long amountCents, String payerMsisdn, String provider, String reference) {
        this.invoiceId = invoiceId;
        this.amountCents = amountCents;
        this.payerMsisdn = payerMsisdn;
        this.provider = provider;
        this.reference = reference;
        this.status = MobileMoneyStatus.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public Long getInvoiceId() {
        return invoiceId;
    }

    public long getAmountCents() {
        return amountCents;
    }

    public String getPayerMsisdn() {
        return payerMsisdn;
    }

    public String getProvider() {
        return provider;
    }

    public String getReference() {
        return reference;
    }

    public String getProviderReference() {
        return providerReference;
    }

    public MobileMoneyStatus getStatus() {
        return status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void markSucceeded(String providerReference) {
        this.status = MobileMoneyStatus.SUCCEEDED;
        this.providerReference = providerReference;
        this.updatedAt = Instant.now();
    }

    public void markFailed(String reason) {
        this.status = MobileMoneyStatus.FAILED;
        this.failureReason = reason;
        this.updatedAt = Instant.now();
    }
}
