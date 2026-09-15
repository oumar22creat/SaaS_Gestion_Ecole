package com.schoolsaas.tenant;

import com.schoolsaas.billing.PlanFeature;
import com.schoolsaas.billing.PlanFeatureService;
import com.schoolsaas.common.ApiException;
import com.schoolsaas.tenant.dto.CustomDomainUpdateRequest;
import com.schoolsaas.tenant.dto.ReportCardTemplateUpdateRequest;
import com.schoolsaas.tenant.dto.TenantBrandingUpdateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Personnalisation par établissement (cahier-des-charges.md §2.3/§2.4, ROADMAP.md 3.7). */
@Service
public class TenantSettingsService {

    private final TenantRepository tenantRepository;
    private final PlanFeatureService planFeatureService;

    public TenantSettingsService(TenantRepository tenantRepository, PlanFeatureService planFeatureService) {
        this.tenantRepository = tenantRepository;
        this.planFeatureService = planFeatureService;
    }

    public Tenant currentTenant() {
        Long tenantId = TenantContext.get();
        if (tenantId == null) {
            throw ApiException.notFound("TENANT_NOT_RESOLVED", "Établissement non résolu");
        }
        return tenantRepository.findById(tenantId).orElseThrow(() -> ApiException.notFound("TENANT_NOT_FOUND", "Établissement introuvable"));
    }

    @Transactional
    public Tenant updateBranding(TenantBrandingUpdateRequest request) {
        Tenant tenant = currentTenant();
        tenant.setLogoUrl(request.logoUrl());
        tenant.setPrimaryColor(request.primaryColor());
        tenant.setSecondaryColor(request.secondaryColor());
        return tenantRepository.save(tenant);
    }

    @Transactional
    public Tenant updateReportCardTemplate(ReportCardTemplateUpdateRequest request) {
        Tenant tenant = currentTenant();
        tenant.setReportCardHeader(request.reportCardHeader());
        tenant.setReportCardLegalMentions(request.reportCardLegalMentions());
        return tenantRepository.save(tenant);
    }

    @Transactional
    public Tenant updateCustomDomain(CustomDomainUpdateRequest request) {
        Tenant tenant = currentTenant();
        if (!planFeatureService.tenantHasFeature(tenant.getId(), PlanFeature.CUSTOM_DOMAIN)) {
            throw ApiException.forbidden(
                    "FEATURE_NOT_INCLUDED", "Le domaine personnalisé n'est pas inclus dans le plan souscrit par cet établissement");
        }
        tenant.setCustomDomain(request.customDomain());
        return tenantRepository.save(tenant);
    }
}
