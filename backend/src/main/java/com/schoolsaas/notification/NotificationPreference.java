package com.schoolsaas.notification;

import com.schoolsaas.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;

/**
 * Préférence d'un utilisateur pour un type de notification (cahier-des-charges.md §16).
 * Aucune ligne pour un (utilisateur, type) donné = notification activée par défaut — voir
 * {@link NotificationPreferenceService}.
 */
@Entity
@Table(name = "notification_preferences")
@Filter(name = "tenantFilter", condition = "school_id = :tenantId")
public class NotificationPreference extends TenantScopedEntity {

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private NotificationType type;

    @Column(nullable = false)
    private boolean enabled;

    protected NotificationPreference() {
    }

    public NotificationPreference(Long userId, NotificationType type, boolean enabled) {
        this.userId = userId;
        this.type = type;
        this.enabled = enabled;
    }

    public Long getUserId() {
        return userId;
    }

    public NotificationType getType() {
        return type;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
