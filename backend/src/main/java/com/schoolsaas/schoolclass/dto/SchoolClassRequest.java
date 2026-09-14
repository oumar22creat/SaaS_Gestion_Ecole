package com.schoolsaas.schoolclass.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SchoolClassRequest(@NotBlank @Size(max = 100) String name, Long headTeacherId) {
}
