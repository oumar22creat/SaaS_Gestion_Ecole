package com.schoolsaas.tenant;

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
 * Un établissement (tenant). Volontairement PAS scopé par school_id : c'est lui-même la
 * racine de l'isolation multi-tenant (voir docs/ARCHITECTURE.md ADR-001).
 */
@Entity
@Table(name = "tenants")
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String subdomain;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenantStatus status;

    @Column(name = "logo_url")
    private String logoUrl;

    /**
     * Clé du logo téléversé, lu localement pour fabriquer les PDF. Distinct de
     * {@code logoUrl}, que le navigateur va chercher lui-même : le serveur ne suit jamais une
     * adresse saisie par un administrateur.
     */
    @Column(name = "logo_storage_key")
    private String logoStorageKey;

    // Couleurs par défaut = design tokens du produit (docs/DESIGN.md §2), identiques au
    // DEFAULT de la colonne côté base (V53__align_tenant_default_branding_with_design_tokens.sql).
    @Column(name = "primary_color", nullable = false)
    private String primaryColor = "#0f5c4c";

    @Column(name = "secondary_color", nullable = false)
    private String secondaryColor = "#c9a227";

    @Column(name = "custom_domain", unique = true)
    private String customDomain;

    @Column(name = "report_card_header")
    private String reportCardHeader;

    @Column(name = "report_card_legal_mentions")
    private String reportCardLegalMentions;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Tenant() {
    }

    public Tenant(String name, String subdomain, TenantStatus status) {
        this.name = name;
        this.subdomain = subdomain;
        this.status = status;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSubdomain() {
        return subdomain;
    }

    public TenantStatus getStatus() {
        return status;
    }

    public void setStatus(TenantStatus status) {
        this.status = status;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getLogoStorageKey() {
        return logoStorageKey;
    }

    public void setLogoStorageKey(String logoStorageKey) {
        this.logoStorageKey = logoStorageKey;
    }

    public String getPrimaryColor() {
        return primaryColor;
    }

    public void setPrimaryColor(String primaryColor) {
        this.primaryColor = primaryColor;
    }

    public String getSecondaryColor() {
        return secondaryColor;
    }

    public void setSecondaryColor(String secondaryColor) {
        this.secondaryColor = secondaryColor;
    }

    public String getCustomDomain() {
        return customDomain;
    }

    public void setCustomDomain(String customDomain) {
        this.customDomain = customDomain;
    }

    public String getReportCardHeader() {
        return reportCardHeader;
    }

    public void setReportCardHeader(String reportCardHeader) {
        this.reportCardHeader = reportCardHeader;
    }

    public String getReportCardLegalMentions() {
        return reportCardLegalMentions;
    }

    public void setReportCardLegalMentions(String reportCardLegalMentions) {
        this.reportCardLegalMentions = reportCardLegalMentions;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
