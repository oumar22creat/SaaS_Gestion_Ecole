package com.schoolsaas.library.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record BookLoanCreateRequest(@NotNull Long studentId, @NotNull LocalDate dueDate) {
}
