package com.schoolsaas.subject.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubjectRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 32) String code,
        @Min(1) int coefficient) {
}
