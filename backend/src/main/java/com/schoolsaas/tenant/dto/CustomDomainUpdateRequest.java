package com.schoolsaas.tenant.dto;

import jakarta.validation.constraints.NotBlank;

public record CustomDomainUpdateRequest(@NotBlank String customDomain) {
}
