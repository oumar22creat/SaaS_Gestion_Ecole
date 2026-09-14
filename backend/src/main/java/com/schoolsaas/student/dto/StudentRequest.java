package com.schoolsaas.student.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record StudentRequest(
        @NotBlank @Size(max = 64) String studentNumber,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        LocalDate birthDate,
        @Size(max = 16) String gender,
        Long schoolClassId) {
}
