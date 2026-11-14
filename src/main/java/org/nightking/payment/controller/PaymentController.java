package org.nightking.payment.controller;

import org.nightking.payment.model.PaymentRequest;
import org.nightking.payment.model.PaymentResponse;
import org.nightking.payment.usecase.ProcessPaymentUseCase;

/** Entry point of the payment service. */
public class PaymentController {
    private final ProcessPaymentUseCase processPayment;

    public PaymentController(ProcessPaymentUseCase processPayment) {
        this.processPayment = processPayment;
    }

    public PaymentResponse processPayment(PaymentRequest request) {
        return processPayment.execute(request);
    }
}
