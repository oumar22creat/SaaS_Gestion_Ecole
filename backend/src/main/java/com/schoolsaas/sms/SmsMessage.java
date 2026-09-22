package com.schoolsaas.sms;

import com.schoolsaas.common.TenantScopedEntity;
import com.schoolsaas.notification.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Filter;

/** Trace d'un SMS, réussi ou non (cahier-des-charges.md §16). */
@Entity
@Table(name = "sms_messages")
@Filter(name = "tenantFilter", condition = "school_id = :tenantId")
public class SmsMessage extends TenantScopedEntity {

    @Column(nullable = false, updatable = false)
    private String recipient;

    /** Le corps réellement envoyé, pas le modèle : c'est lui qui fait foi en cas de litige. */
    @Column(nullable = false, updatable = false)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", updatable = false)
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SmsStatus status;

    @Column(nullable = false, updatable = false)
    private String provider;

    @Column(name = "provider_reference")
    private String providerReference;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected SmsMessage() {
    }

    public SmsMessage(String recipient, String body, NotificationType notificationType, String provider) {
        this.recipient = recipient;
        this.body = body;
        this.notificationType = notificationType;
        this.provider = provider;
        this.status = SmsStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public String getRecipient() {
        return recipient;
    }

    public String getBody() {
        return body;
    }

    public NotificationType getNotificationType() {
        return notificationType;
    }

    public SmsStatus getStatus() {
        return status;
    }

    public void markSent(String providerReference) {
        this.status = SmsStatus.SENT;
        this.providerReference = providerReference;
    }

    public void markFailed(String reason) {
        this.status = SmsStatus.FAILED;
        // La colonne est bornée : une trace tronquée vaut mieux qu'un échec d'enregistrement
        // qui ferait perdre la raison de l'échec en plus du SMS.
        this.failureReason = reason == null ? null : reason.substring(0, Math.min(reason.length(), 500));
    }

    public String getProviderReference() {
        return providerReference;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
