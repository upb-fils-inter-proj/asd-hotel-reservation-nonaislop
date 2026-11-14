package org.nightking.payment.presenter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.nightking.payment.model.PaymentResponse;

/** JSON shape of a payment result returned by the API. */
public record PaymentViewModel(String reservationId, BigDecimal amount, String currency, String status) {

    static PaymentViewModel from(PaymentResponse payment) {
        return new PaymentViewModel(
                payment.reservationId(),
                BigDecimal.valueOf(payment.amount()).setScale(2, RoundingMode.HALF_UP),
                "EUR",
                payment.approved() ? "APPROVED" : "DECLINED");
    }
}
