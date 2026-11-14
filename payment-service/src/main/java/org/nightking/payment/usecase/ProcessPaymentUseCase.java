package org.nightking.payment.usecase;

import org.nightking.payment.model.PaymentRequest;

/** Charges a guest for a reservation through the configured payment provider. */
public interface ProcessPaymentUseCase {
    void execute(PaymentRequest request, ProcessPaymentPresenter presenter);
}
