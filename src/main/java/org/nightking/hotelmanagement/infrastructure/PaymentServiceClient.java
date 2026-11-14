package org.nightking.hotelmanagement.infrastructure;

import org.nightking.hotelmanagement.gateway.PaymentGateway;
import org.nightking.payment.controller.PaymentController;
import org.nightking.payment.model.PaymentRequest;

/**
 * Reaches the payment service through its controller. This is the only class in hotel management
 * that knows the payment service exists, so moving it behind HTTP only changes this class.
 */
public class PaymentServiceClient implements PaymentGateway {
    private final PaymentController payment;

    public PaymentServiceClient(PaymentController payment) {
        this.payment = payment;
    }

    @Override
    public boolean pay(String reservationId, double amount) {
        return payment.processPayment(new PaymentRequest(reservationId, amount)).approved();
    }
}
