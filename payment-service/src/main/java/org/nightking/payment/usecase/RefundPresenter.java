package org.nightking.payment.usecase;

import org.nightking.payment.model.PaymentResponse;

/** Output port of the refund use case. */
public interface RefundPresenter {
    void presentRefunded(PaymentResponse payment);
    void presentNotFound(String message);
    void presentRejected(String message);
}
