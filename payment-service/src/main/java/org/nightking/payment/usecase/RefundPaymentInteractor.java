package org.nightking.payment.usecase;

import java.util.Optional;
import org.nightking.payment.entity.Payment;
import org.nightking.payment.gateway.PaymentProcessor;
import org.nightking.payment.gateway.PaymentRepository;
import org.nightking.payment.model.PaymentResponse;

public class RefundPaymentInteractor implements RefundPaymentUseCase {
    private final PaymentProcessor processor;
    private final PaymentRepository payments;

    public RefundPaymentInteractor(PaymentProcessor processor, PaymentRepository payments) {
        this.processor = processor;
        this.payments = payments;
    }

    @Override
    public void execute(String transactionId, RefundPresenter presenter) {
        Optional<Payment> found = payments.findByTransactionId(transactionId);
        if (found.isEmpty()) {
            presenter.presentNotFound("Payment " + transactionId + " not found.");
            return;
        }
        Payment payment = found.get();
        if (!payment.canBeRefunded()) {
            presenter.presentRejected("Payment " + transactionId + " is " + payment.getStatus() + " and cannot be refunded.");
            return;
        }
        if (!processor.refund(transactionId, payment.getAmount())) {
            presenter.presentRejected("The card network refused the refund for " + transactionId + ".");
            return;
        }
        payment.markRefunded();
        payments.save(payment);
        System.out.printf("Refunded payment %s: EUR %.2f%n", transactionId, payment.getAmount());
        presenter.presentRefunded(PaymentResponse.from(payment));
    }
}
