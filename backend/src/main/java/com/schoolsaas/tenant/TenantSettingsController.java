package com.schoolsaas.tenant;

import com.schoolsaas.common.ApiResponse;
import com.schoolsaas.tenant.dto.CustomDomainResponse;
import com.schoolsaas.tenant.dto.CustomDomainUpdateRequest;
import com.schoolsaas.tenant.dto.ReportCardTemplateResponse;
import com.schoolsaas.tenant.dto.ReportCardTemplateUpdateRequest;
import com.schoolsaas.tenant.dto.TenantBrandingResponse;
import com.schoolsaas.tenant.dto.TenantBrandingUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Personnalisation par établissement — cahier-des-charges.md §2.3/§2.4, ROADMAP.md 3.7.
 * {@code GET /branding} est volontairement public (voir SecurityConfig) : Web et Mobile
 * l'appellent au démarrage, avant toute connexion, pour appliquer le branding du tenant résolu
 * par sous-domaine/en-tête (voir TenantResolver) — auparavant absent, voir ADR-015 (Phase 1) et
 * ADR-028 (résolu ici).
 */
@RestController
@RequestMapping("/api/v1/tenants/current")
public class TenantSettingsController {

    private final TenantSettingsService tenantSettingsService;

    public TenantSettingsController(TenantSettingsService tenantSettingsService) {
        this.tenantSettingsService = tenantSettingsService;
    }

    @GetMapping("/branding")
    public TenantBrandingResponse branding() {
        return TenantBrandingResponse.from(tenantSettingsService.currentTenant());
    }

    @PutMapping("/branding")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION')")
    public ApiResponse<TenantBrandingResponse> updateBranding(@Valid @RequestBody TenantBrandingUpdateRequest request) {
        return ApiResponse.of(TenantBrandingResponse.from(tenantSettingsService.updateBranding(request)));
    }

    @GetMapping("/report-card-template")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION')")
    public ApiResponse<ReportCardTemplateResponse> reportCardTemplate() {
        return ApiResponse.of(ReportCardTemplateResponse.from(tenantSettingsService.currentTenant()));
    }

    @PutMapping("/report-card-template")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION')")
    public ApiResponse<ReportCardTemplateResponse> updateReportCardTemplate(@RequestBody ReportCardTemplateUpdateRequest request) {
        return ApiResponse.of(ReportCardTemplateResponse.from(tenantSettingsService.updateReportCardTemplate(request)));
    }

    @GetMapping("/custom-domain")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION')")
    public ApiResponse<CustomDomainResponse> customDomain() {
        return ApiResponse.of(CustomDomainResponse.from(tenantSettingsService.currentTenant()));
    }

    @PutMapping("/custom-domain")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION')")
    public ApiResponse<CustomDomainResponse> updateCustomDomain(@Valid @RequestBody CustomDomainUpdateRequest request) {
        return ApiResponse.of(CustomDomainResponse.from(tenantSettingsService.updateCustomDomain(request)));
    }
}
