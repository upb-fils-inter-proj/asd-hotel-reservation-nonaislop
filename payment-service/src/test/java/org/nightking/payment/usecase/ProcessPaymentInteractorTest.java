package org.nightking.payment.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.nightking.payment.entity.Payment;
import org.nightking.payment.gateway.PaymentRepository;
import org.nightking.payment.infrastructure.PayFlowGateway;
import org.nightking.payment.infrastructure.PayFlowGatewayAdapter;
import org.nightking.payment.model.CardRequest;
import org.nightking.payment.model.PaymentRequest;
import org.nightking.payment.model.PaymentResponse;

/** Charges and refunds through the simulated PayFlow card network, with an in-memory repository. */
class ProcessPaymentInteractorTest {
    private static final Clock NOVEMBER_2026 = Clock.fixed(Instant.parse("2026-11-14T10:00:00Z"), ZoneOffset.UTC);

    static class Payments implements PaymentRepository {
        final Map<String, Payment> saved = new HashMap<>();
        @Override public void save(Payment payment) { saved.put(payment.getTransactionId(), payment); }
        @Override public Optional<Payment> findByTransactionId(String id) { return Optional.ofNullable(saved.get(id)); }
    }

    static class RecordingPresenter implements ProcessPaymentPresenter, RefundPresenter {
        PaymentResponse result;
        String problem;

        @Override public void presentResult(PaymentResponse payment) { result = payment; }
        @Override public void presentInvalidRequest(String reason) { problem = reason; }
        @Override public void presentRefunded(PaymentResponse payment) { result = payment; }
        @Override public void presentNotFound(String message) { problem = message; }
        @Override public void presentRejected(String message) { problem = message; }
    }

    private final Payments payments = new Payments();
    private final PayFlowGatewayAdapter payFlow = new PayFlowGatewayAdapter(new PayFlowGateway());
    private final ProcessPaymentUseCase charge = new ProcessPaymentInteractor(payFlow, payments, NOVEMBER_2026);
    private final RefundPaymentUseCase refund = new RefundPaymentInteractor(payFlow, payments);

    private RecordingPresenter pay(String cardNumber, int expiryMonth, int expiryYear, double amount) {
        RecordingPresenter presenter = new RecordingPresenter();
        charge.execute(new PaymentRequest("RES-1000", amount,
                new CardRequest("Alex Morgan", cardNumber, expiryMonth, expiryYear, "123")), presenter);
        return presenter;
    }

    private RecordingPresenter pay(String cardNumber) {
        return pay(cardNumber, 12, 2028, 450.0);
    }

    @Test
    void approvesTheSuccessTestCardAndKeepsOnlyTheLastFourDigits() {
        PaymentResponse result = pay("4242 4242 4242 4242").result;

        assertEquals("APPROVED", result.status());
        assertTrue(result.transactionId().startsWith("PAY-"));
        assertEquals("Visa", result.cardBrand());
        assertEquals("4242", result.cardLastFour());
        assertEquals("APPROVED", payments.saved.get(result.transactionId()).getStatus().name());
    }

    @Test
    void networkDeclinesCarryAReason() {
        assertEquals("Your card was declined.", pay("4000 0000 0000 0002").result.declineReason());
        assertEquals("Insufficient funds.", pay("4000 0000 0000 9995").result.declineReason());
        assertEquals("The card has expired.", pay("4000 0000 0000 0069").result.declineReason());
        assertEquals(3, payments.saved.values().stream().filter(p -> p.getStatus().name().equals("DECLINED")).count());
    }

    @Test
    void invalidCardsAreDeclinedBeforeReachingTheNetwork() {
        PaymentResponse badNumber = pay("4242 4242 4242 4241").result;
        assertEquals("DECLINED", badNumber.status());
        assertEquals("Card number is not valid.", badNumber.declineReason());
        assertEquals("The card has expired.", pay("4242 4242 4242 4242", 10, 2026, 450.0).result.declineReason());
    }

    @Test
    void missingFieldsAreInvalidRequests() {
        RecordingPresenter noCard = new RecordingPresenter();
        charge.execute(new PaymentRequest("RES-1000", 450.0, null), noCard);
        assertNotNull(noCard.problem);
        assertNull(noCard.result);
        assertNotNull(pay("4242 4242 4242 4242", 12, 2028, 0).problem);
    }

    @Test
    void approvedPaymentsCanBeRefundedOnce() {
        String id = pay("4242 4242 4242 4242").result.transactionId();

        RecordingPresenter first = new RecordingPresenter();
        refund.execute(id, first);
        assertEquals("REFUNDED", first.result.status());

        RecordingPresenter second = new RecordingPresenter();
        refund.execute(id, second);
        assertNotNull(second.problem);
    }

    @Test
    void declinedOrUnknownPaymentsCannotBeRefunded() {
        String declined = pay("4000 0000 0000 0002").result.transactionId();
        RecordingPresenter presenter = new RecordingPresenter();
        refund.execute(declined, presenter);
        assertNotNull(presenter.problem);

        RecordingPresenter unknown = new RecordingPresenter();
        refund.execute("PAY-NOPE", unknown);
        assertEquals("Payment PAY-NOPE not found.", unknown.problem);
    }
}
