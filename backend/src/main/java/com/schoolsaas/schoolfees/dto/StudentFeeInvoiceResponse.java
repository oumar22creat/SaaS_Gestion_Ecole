package com.schoolsaas.schoolfees.dto;

import com.schoolsaas.schoolfees.FeeInvoiceStatus;
import com.schoolsaas.schoolfees.StudentFeeInvoice;
import java.time.Instant;

public record StudentFeeInvoiceResponse(
        Long id,
        Long feeScheduleId,
        Long studentId,
        /** Montant net dû, remise déduite. */
        long amountDueCents,
        /** Réduction accordée sur le montant de l'échéancier, et son motif. */
        long discountCents,
        String discountReason,
        long amountPaidCents,
        FeeInvoiceStatus status,
        Instant issuedAt) {

    public static StudentFeeInvoiceResponse from(StudentFeeInvoice invoice, long amountPaidCents) {
        return new StudentFeeInvoiceResponse(
                invoice.getId(), invoice.getFeeScheduleId(), invoice.getStudentId(), invoice.getAmountDueCents(),
                invoice.getDiscountCents(),
                invoice.getDiscountReason(),
                amountPaidCents, invoice.getStatus(), invoice.getIssuedAt());
    }
}
