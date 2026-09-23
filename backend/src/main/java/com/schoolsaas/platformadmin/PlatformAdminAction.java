package com.schoolsaas.platformadmin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Trace d'une action de la console plateforme. Volontairement hors {@code TenantScopedEntity} :
 * le journal appartient à l'éditeur, pas aux établissements qu'il décrit.
 */
@Entity
@Table(name = "platform_admin_actions")
public class PlatformAdminAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "platform_admin_id", nullable = false, updatable = false)
    private Long platformAdminId;

    @Column(name = "tenant_id", updatable = false)
    private Long tenantId;

    @Column(nullable = false, updatable = false)
    private String action;

    /** Ce qui a changé, en clair : « TRIAL → SUSPENDED ». */
    @Column(updatable = false)
    private String detail;

    @Column(updatable = false)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PlatformAdminAction() {
    }

    public PlatformAdminAction(Long platformAdminId, Long tenantId, String action, String detail, String reason) {
        this.platformAdminId = platformAdminId;
        this.tenantId = tenantId;
        this.action = action;
        this.detail = detail;
        this.reason = reason;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getPlatformAdminId() {
        return platformAdminId;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public String getAction() {
        return action;
    }

    public String getDetail() {
        return detail;
    }

    public String getReason() {
        return reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
