package com.schoolsaas.tenant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TenantBrandingUpdateRequest(
        String logoUrl,
        @NotBlank @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Couleur hexadécimale attendue, ex. #3880ff") String primaryColor,
        @NotBlank @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Couleur hexadécimale attendue, ex. #3dc2ff") String secondaryColor) {
}
