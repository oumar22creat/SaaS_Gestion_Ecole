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
 * Appareil joignable par notification push, rattaché au compte qui s'y est connecté
 * (docs/NOTIFICATIONS-PUSH.md).
 *
 * <p>Un jeton ne vaut que pour un compte à la fois. Sur un téléphone partagé — celui d'une
 * école, celui d'un foyer — un enseignant se déconnecte et un parent se connecte : les
 * notifications du premier ne doivent pas continuer d'arriver sur un écran que lit désormais
 * le second. D'où {@link #reattribuerA} plutôt qu'une seconde ligne.
 */
@Entity
@Table(name = "device_tokens")
@Filter(name = "tenantFilter", condition = "school_id = :tenantId")
public class DeviceToken extends TenantScopedEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, updatable = false)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DevicePlatform platform;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    protected DeviceToken() {
    }

    public DeviceToken(Long userId, String token, DevicePlatform platform) {
        this.userId = userId;
        this.token = token;
        this.platform = platform;
        this.createdAt = Instant.now();
        this.lastSeenAt = this.createdAt;
    }

    /** Fait passer l'appareil au compte qui vient de s'y connecter. */
    public void reattribuerA(Long userId, DevicePlatform platform) {
        this.userId = userId;
        this.platform = platform;
        this.lastSeenAt = Instant.now();
    }

    public Long getUserId() {
        return userId;
    }

    public String getToken() {
        return token;
    }

    public DevicePlatform getPlatform() {
        return platform;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }
}
