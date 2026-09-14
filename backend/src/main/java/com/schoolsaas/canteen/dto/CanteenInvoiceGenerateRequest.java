package com.schoolsaas.canteen.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public record CanteenInvoiceGenerateRequest(
        @NotNull Long studentId, @NotNull LocalDate periodFrom, @NotNull LocalDate periodTo, @Positive long pricePerMealCents) {
}
