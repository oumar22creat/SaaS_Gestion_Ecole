package com.schoolsaas.schoolfees.dto;

import com.schoolsaas.schoolfees.FeePayment;
import com.schoolsaas.schoolfees.FeePaymentMethod;
import java.time.Instant;

public record FeePaymentResponse(
        Long id, Long invoiceId, long amountCents, FeePaymentMethod method, String reference, Long recordedByUserId, Instant paidAt) {

    public static FeePaymentResponse from(FeePayment payment) {
        return new FeePaymentResponse(
                payment.getId(), payment.getInvoiceId(), payment.getAmountCents(), payment.getMethod(),
                payment.getReference(), payment.getRecordedByUserId(), payment.getPaidAt());
    }
}
