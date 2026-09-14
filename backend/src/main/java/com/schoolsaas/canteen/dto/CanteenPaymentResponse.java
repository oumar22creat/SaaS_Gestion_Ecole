package com.schoolsaas.canteen.dto;

import com.schoolsaas.canteen.CanteenPayment;
import com.schoolsaas.schoolfees.FeePaymentMethod;
import java.time.Instant;

public record CanteenPaymentResponse(
        Long id, Long invoiceId, long amountCents, FeePaymentMethod method, String reference, Long recordedByUserId, Instant paidAt) {

    public static CanteenPaymentResponse from(CanteenPayment payment) {
        return new CanteenPaymentResponse(
                payment.getId(), payment.getInvoiceId(), payment.getAmountCents(), payment.getMethod(),
                payment.getReference(), payment.getRecordedByUserId(), payment.getPaidAt());
    }
}
