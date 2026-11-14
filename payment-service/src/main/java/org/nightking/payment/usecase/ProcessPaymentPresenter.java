package org.nightking.payment.usecase;

import org.nightking.payment.model.PaymentResponse;

/** Output port of the process-payment use case. */
public interface ProcessPaymentPresenter {
    void presentResult(PaymentResponse payment);
    void presentInvalidRequest(String reason);
}
