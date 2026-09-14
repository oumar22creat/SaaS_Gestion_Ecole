package com.schoolsaas.canteen.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record MealReservationCreateRequest(@NotNull Long studentId, @NotNull LocalDate date, boolean specialDiet) {
}
