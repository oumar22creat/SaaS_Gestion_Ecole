package com.schoolsaas.canteen.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record MenuCreateRequest(@NotNull LocalDate date, @NotBlank String mainDescription, String specialDietDescription) {
}
