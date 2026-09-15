package com.schoolsaas.notification;

import com.schoolsaas.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Filter;

/**
 * Personnalisation par établissement du titre/corps d'un {@link NotificationType} (cahier
 * §2.4, ROADMAP.md 3.7, voir docs/ARCHITECTURE.md ADR-028). Aucune ligne pour un
 * (school_id, type) donné = message par défaut du module appelant inchangé (voir
 * {@link NotificationDispatcher}), même principe que {@link NotificationPreference}.
 */
@Entity
@Table(name = "notification_templates")
@Filter(name = "tenantFilter", condition = "school_id = :tenantId")
public class NotificationTemplate extends TenantScopedEntity {

    /** Placeholder obligatoire dans {@link #bodyTemplate} : remplacé par le message calculé par le module appelant. */
    public static final String MESSAGE_PLACEHOLDER = "{message}";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private NotificationType type;

    @Column(name = "title_override")
    private String titleOverride;

    @Column(name = "body_template")
    private String bodyTemplate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected NotificationTemplate() {
    }

    public NotificationTemplate(NotificationType type) {
        this.type = type;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public NotificationType getType() {
        return type;
    }

    public String getTitleOverride() {
        return titleOverride;
    }

    public String getBodyTemplate() {
        return bodyTemplate;
    }

    public void setTitleOverride(String titleOverride) {
        this.titleOverride = titleOverride;
        this.updatedAt = Instant.now();
    }

    public void setBodyTemplate(String bodyTemplate) {
        this.bodyTemplate = bodyTemplate;
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
