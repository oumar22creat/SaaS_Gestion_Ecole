package com.schoolsaas.tenant.dto;

import com.schoolsaas.auth.dto.TokenPairResponse;

public record TenantRegistrationResponse(Long tenantId, String subdomain, TokenPairResponse tokens) {
}
