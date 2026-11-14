package org.nightking.payment.model;

import org.nightking.payment.entity.Payment;

public record PaymentResponse(String transactionId,
                              String reservationId,
                              double amount,
                              String status,
                              String declineReason,
                              String cardBrand,
                              String cardLastFour) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(payment.getTransactionId(), payment.getReservationId(), payment.getAmount(),
                payment.getStatus().name(), payment.getDeclineReason(), payment.getCardBrand(),
                payment.getCardLastFour());
    }
}
