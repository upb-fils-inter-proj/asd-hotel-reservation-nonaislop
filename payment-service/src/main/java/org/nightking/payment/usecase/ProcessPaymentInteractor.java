package org.nightking.payment.usecase;

import java.time.Clock;
import java.time.YearMonth;
import java.util.UUID;
import org.nightking.payment.entity.Payment;
import org.nightking.payment.entity.PaymentCard;
import org.nightking.payment.gateway.PaymentProcessor;
import org.nightking.payment.gateway.PaymentProcessor.ChargeOutcome;
import org.nightking.payment.gateway.PaymentRepository;
import org.nightking.payment.model.CardRequest;
import org.nightking.payment.model.PaymentRequest;
import org.nightking.payment.model.PaymentResponse;

public class ProcessPaymentInteractor implements ProcessPaymentUseCase {
    private final PaymentProcessor processor;
    private final PaymentRepository payments;
    private final Clock clock;

    public ProcessPaymentInteractor(PaymentProcessor processor, PaymentRepository payments, Clock clock) {
        this.processor = processor;
        this.payments = payments;
        this.clock = clock;
    }

    @Override
    public void execute(PaymentRequest request, ProcessPaymentPresenter presenter) {
        if (request.reservationId() == null || request.reservationId().isBlank()) {
            presenter.presentInvalidRequest("reservationId is required.");
            return;
        }
        if (request.amount() <= 0) {
            presenter.presentInvalidRequest("amount must be greater than zero.");
            return;
        }
        if (request.card() == null) {
            presenter.presentInvalidRequest("card details are required.");
            return;
        }

        CardRequest details = request.card();
        PaymentCard card = new PaymentCard(details.holderName(), details.number(),
                details.expiryMonth(), details.expiryYear(), details.cvc());
        Payment payment = new Payment("PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                request.reservationId(), request.amount(), card.brand(), card.lastFour());
        System.out.printf("Processing payment %s: EUR %.2f with %s%n", payment.getTransactionId(), payment.getAmount(), card);

        // A card that can't be valid is declined here, without bothering the card network.
        String problem = card.validationProblem(YearMonth.now(clock));
        ChargeOutcome outcome = problem != null
                ? ChargeOutcome.declined(problem)
                : processor.charge(payment.getTransactionId(), payment.getReservationId(), payment.getAmount(), card);

        if (outcome.approved()) {
            payment.approve();
        } else {
            payment.decline(outcome.declineReason());
        }
        payments.save(payment);
        presenter.presentResult(PaymentResponse.from(payment));
    }
}
