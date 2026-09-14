package com.schoolsaas.billing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Facture synchronisée depuis Stripe (webhook). Entité plateforme, filtrage par tenant
 * manuel dans {@link InvoiceRepository} — voir {@link Subscription} pour la même remarque.
 */
@Entity
@Table(name = "invoices")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    @Column(name = "subscription_id", nullable = false, updatable = false)
    private Long subscriptionId;

    @Column(name = "stripe_invoice_id", nullable = false, unique = true, updatable = false)
    private String stripeInvoiceId;

    @Column(name = "amount_due", nullable = false)
    private Integer amountDue;

    @Column(nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status;

    @Column(name = "period_start")
    private Instant periodStart;

    @Column(name = "period_end")
    private Instant periodEnd;

    @Column(name = "hosted_invoice_url")
    private String hostedInvoiceUrl;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Invoice() {
    }

    public Invoice(
            Long tenantId,
            Long subscriptionId,
            String stripeInvoiceId,
            Integer amountDue,
            String currency,
            InvoiceStatus status,
            Instant periodStart,
            Instant periodEnd,
            String hostedInvoiceUrl) {
        this.tenantId = tenantId;
        this.subscriptionId = subscriptionId;
        this.stripeInvoiceId = stripeInvoiceId;
        this.amountDue = amountDue;
        this.currency = currency;
        this.status = status;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.hostedInvoiceUrl = hostedInvoiceUrl;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public Long getSubscriptionId() {
        return subscriptionId;
    }

    public String getStripeInvoiceId() {
        return stripeInvoiceId;
    }

    public Integer getAmountDue() {
        return amountDue;
    }

    public String getCurrency() {
        return currency;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public void setStatus(InvoiceStatus status) {
        this.status = status;
    }

    public Instant getPeriodStart() {
        return periodStart;
    }

    public Instant getPeriodEnd() {
        return periodEnd;
    }

    public String getHostedInvoiceUrl() {
        return hostedInvoiceUrl;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(Instant paidAt) {
        this.paidAt = paidAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
