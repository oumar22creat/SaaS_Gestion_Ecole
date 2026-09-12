package com.schoolsaas.tenant.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TenantRegistrationRequest(
        @NotBlank @Size(max = 255) String schoolName,

        @NotBlank
        @Pattern(
                regexp = "^[a-z0-9](?:[a-z0-9-]{1,61}[a-z0-9])?$",
                message = "3 à 63 caractères, lettres minuscules/chiffres/tirets uniquement")
        String subdomain,

        @NotBlank @Email String adminEmail,

        @NotBlank @Size(min = 8, max = 100, message = "8 caractères minimum") String adminPassword,

        @NotBlank @Size(max = 100) String adminFirstName,

        @NotBlank @Size(max = 100) String adminLastName) {
}
