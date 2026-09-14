package com.schoolsaas.schoolfees.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public record FeeScheduleCreateRequest(
        @NotNull Long schoolClassId, @NotBlank String label, @Positive long amountCents, @NotNull LocalDate dueDate) {
}
