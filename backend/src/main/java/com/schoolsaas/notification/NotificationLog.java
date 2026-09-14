package com.schoolsaas.notification;

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
 * Journal des notifications réellement envoyées — cahier-des-charges.md §18 ("usage global"
 * du tableau de bord Super-Admin, ADR-021). Entité plateforme, pas scopée school_id (comme
 * {@code Subscription}/{@code Invoice}) : c'est un compteur d'usage global, pas une donnée
 * métier à isoler par tenant.
 */
@Entity
@Table(name = "notification_log")
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private NotificationType type;

    @Column(name = "recipient_count", nullable = false, updatable = false)
    private int recipientCount;

    @Column(name = "sent_at", nullable = false, updatable = false)
    private Instant sentAt;

    protected NotificationLog() {
    }

    public NotificationLog(Long tenantId, NotificationType type, int recipientCount) {
        this.tenantId = tenantId;
        this.type = type;
        this.recipientCount = recipientCount;
        this.sentAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public NotificationType getType() {
        return type;
    }

    public int getRecipientCount() {
        return recipientCount;
    }

    public Instant getSentAt() {
        return sentAt;
    }
}
