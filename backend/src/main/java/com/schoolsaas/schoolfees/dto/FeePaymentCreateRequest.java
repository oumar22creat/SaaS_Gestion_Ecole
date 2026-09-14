package com.schoolsaas.schoolfees.dto;

import com.schoolsaas.schoolfees.FeePaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record FeePaymentCreateRequest(@Positive long amountCents, @NotNull FeePaymentMethod method, String reference) {
}
