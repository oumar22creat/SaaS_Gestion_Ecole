package com.schoolsaas.canteen.dto;

import com.schoolsaas.canteen.CanteenInvoice;
import com.schoolsaas.canteen.CanteenInvoiceStatus;
import java.time.LocalDate;

public record CanteenInvoiceResponse(
        Long id,
        Long studentId,
        LocalDate periodFrom,
        LocalDate periodTo,
        int mealCount,
        long pricePerMealCents,
        long amountDueCents,
        long amountPaidCents,
        CanteenInvoiceStatus status) {

    public static CanteenInvoiceResponse from(CanteenInvoice invoice, long amountPaidCents) {
        return new CanteenInvoiceResponse(
                invoice.getId(), invoice.getStudentId(), invoice.getPeriodFrom(), invoice.getPeriodTo(), invoice.getMealCount(),
                invoice.getPricePerMealCents(), invoice.getAmountDueCents(), amountPaidCents, invoice.getStatus());
    }
}
