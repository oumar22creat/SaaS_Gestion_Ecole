package com.schoolsaas.student.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * {@code studentNumber} est facultatif : l'application l'attribue elle-même
 * ({@link com.schoolsaas.student.StudentNumberGenerator}). Le champ subsiste pour l'import
 * d'un effectif existant, où l'établissement conserve sa propre numérotation.
 */
public record StudentRequest(
        @Size(max = 64) String studentNumber,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        LocalDate birthDate,
        @Size(max = 16) String gender,
        Long schoolClassId) {
}
