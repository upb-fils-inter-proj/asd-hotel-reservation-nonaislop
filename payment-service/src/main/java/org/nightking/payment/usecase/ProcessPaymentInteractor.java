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
    public void execute(PaymentRequest request, ProcessPaymentPresenter presenter) {
        if (request.reservationId() == null || request.reservationId().isBlank()) {
            presenter.presentInvalidRequest("reservationId is required.");
            return;
        }

        Payment payment = new Payment(request.reservationId(), request.amount());
        System.out.printf("Processing payment: EUR %.2f%n", payment.getAmount());

        if (processor.processPayment(payment.getReservationId(), payment.getAmount())) {
            payment.approve();
        } else {
            payment.decline();
        }

        presenter.presentResult(new PaymentResponse(payment.getReservationId(), payment.getAmount(),
                payment.getStatus() == PaymentStatus.APPROVED));
    }
}
