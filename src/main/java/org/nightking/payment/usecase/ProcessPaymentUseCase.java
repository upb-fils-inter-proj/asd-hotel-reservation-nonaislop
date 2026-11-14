package org.nightking.payment.usecase;

import org.nightking.payment.model.PaymentRequest;
import org.nightking.payment.model.PaymentResponse;

/** Charges a guest for a reservation through the configured payment provider. */
public interface ProcessPaymentUseCase {
    PaymentResponse execute(PaymentRequest request);
}
