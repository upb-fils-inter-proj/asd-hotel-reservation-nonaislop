package org.nightking.payment.usecase;

import org.nightking.payment.entity.Payment;
import org.nightking.payment.entity.PaymentStatus;
import org.nightking.payment.gateway.PaymentProcessor;
import org.nightking.payment.model.PaymentRequest;
import org.nightking.payment.model.PaymentResponse;

public class ProcessPaymentInteractor implements ProcessPaymentUseCase {
    private final PaymentProcessor processor;

    public ProcessPaymentInteractor(PaymentProcessor processor) {
        this.processor = processor;
    }

    @Override
    public PaymentResponse execute(PaymentRequest request) {
        Payment payment = new Payment(request.reservationId(), request.amount());
        System.out.printf("Processing payment: EUR %.2f%n", payment.getAmount());

        if (processor.processPayment(payment.getReservationId(), payment.getAmount())) {
            payment.approve();
        } else {
            payment.decline();
        }

        return new PaymentResponse(payment.getReservationId(), payment.getAmount(),
                payment.getStatus() == PaymentStatus.APPROVED);
    }
}
