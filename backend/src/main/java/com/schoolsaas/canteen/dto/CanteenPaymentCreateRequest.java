package com.schoolsaas.canteen.dto;

import com.schoolsaas.schoolfees.FeePaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CanteenPaymentCreateRequest(@Positive long amountCents, @NotNull FeePaymentMethod method, String reference) {
}
