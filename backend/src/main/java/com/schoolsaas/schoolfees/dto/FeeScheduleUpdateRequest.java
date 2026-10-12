package com.schoolsaas.schoolfees.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

/**
 * Correction d'une échéance déjà créée.
 *
 * <p>La classe n'est pas modifiable : une échéance appartient à une classe, et la déplacer
 * reviendrait à facturer d'autres élèves que ceux déjà facturés. Pour cela, on en crée une
 * autre.
 */
public record FeeScheduleUpdateRequest(
        @NotBlank String label,
        @Positive long amountCents,
        @NotNull LocalDate dueDate) {
}
