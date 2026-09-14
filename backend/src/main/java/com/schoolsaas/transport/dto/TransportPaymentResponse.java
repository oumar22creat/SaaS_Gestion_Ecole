package com.schoolsaas.transport.dto;

import com.schoolsaas.schoolfees.FeePaymentMethod;
import com.schoolsaas.transport.TransportPayment;
import java.time.Instant;

public record TransportPaymentResponse(
        Long id, Long invoiceId, long amountCents, FeePaymentMethod method, String reference, Long recordedByUserId, Instant paidAt) {

    public static TransportPaymentResponse from(TransportPayment payment) {
        return new TransportPaymentResponse(
                payment.getId(), payment.getInvoiceId(), payment.getAmountCents(), payment.getMethod(),
                payment.getReference(), payment.getRecordedByUserId(), payment.getPaidAt());
    }
}
