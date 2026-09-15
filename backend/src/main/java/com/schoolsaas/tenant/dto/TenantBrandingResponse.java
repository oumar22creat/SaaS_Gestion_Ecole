package com.schoolsaas.tenant.dto;

import com.schoolsaas.tenant.Tenant;

/** Doit rester compatible avec web/src/app/branding/tenant-branding.model.ts (TenantBranding). */
public record TenantBrandingResponse(String name, String logoUrl, String primaryColor, String secondaryColor) {

    public static TenantBrandingResponse from(Tenant tenant) {
        return new TenantBrandingResponse(tenant.getName(), tenant.getLogoUrl(), tenant.getPrimaryColor(), tenant.getSecondaryColor());
    }
}
