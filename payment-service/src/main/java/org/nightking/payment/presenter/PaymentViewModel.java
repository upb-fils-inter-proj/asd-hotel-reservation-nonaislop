package org.nightking.payment.presenter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.nightking.payment.model.PaymentResponse;

/** JSON shape of a payment returned by the API. Only the card brand and last four digits are shown. */
public record PaymentViewModel(String transactionId,
                               String reservationId,
                               BigDecimal amount,
                               String currency,
                               String status,
                               String declineReason,
                               String card) {

    static PaymentViewModel from(PaymentResponse payment) {
        return new PaymentViewModel(
                payment.transactionId(),
                payment.reservationId(),
                BigDecimal.valueOf(payment.amount()).setScale(2, RoundingMode.HALF_UP),
                "EUR",
                payment.status(),
                payment.declineReason(),
                payment.cardBrand() + " •••• " + payment.cardLastFour());
    }
}
