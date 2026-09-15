package com.schoolsaas.tenant.dto;

import com.schoolsaas.tenant.Tenant;

public record CustomDomainResponse(String customDomain) {

    public static CustomDomainResponse from(Tenant tenant) {
        return new CustomDomainResponse(tenant.getCustomDomain());
    }
}
