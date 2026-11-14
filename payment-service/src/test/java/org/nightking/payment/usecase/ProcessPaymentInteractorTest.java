package org.nightking.payment.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.nightking.payment.gateway.PaymentProcessor;
import org.nightking.payment.infrastructure.PayFlowGateway;
import org.nightking.payment.infrastructure.PayFlowGatewayAdapter;
import org.nightking.payment.model.PaymentRequest;
import org.nightking.payment.model.PaymentResponse;

class ProcessPaymentInteractorTest {

    static class RecordingPresenter implements ProcessPaymentPresenter {
        PaymentResponse result;
        String invalidReason;

        @Override public void presentResult(PaymentResponse payment) { this.result = payment; }
        @Override public void presentInvalidRequest(String reason) { this.invalidReason = reason; }
    }

    private static RecordingPresenter pay(PaymentProcessor processor, String reservationId, double amount) {
        RecordingPresenter presenter = new RecordingPresenter();
        new ProcessPaymentInteractor(processor).execute(new PaymentRequest(reservationId, amount), presenter);
        return presenter;
    }

    private static PaymentProcessor payFlow() {
        return new PayFlowGatewayAdapter(new PayFlowGateway());
    }

    @Test
    void approvesPositiveAmount() {
        PaymentResponse result = pay(payFlow(), "RES-1", 450.0).result;
        assertTrue(result.approved());
        assertEquals("RES-1", result.reservationId());
        assertEquals(450.0, result.amount(), 1e-9);
    }

    @Test
    void declinesZeroAmount() {
        assertFalse(pay(payFlow(), "RES-1", 0.0).result.approved());
    }

    @Test
    void passesReservationAndAmountToTheProvider() {
        List<String> calls = new ArrayList<>();
        assertTrue(pay((id, amount) -> calls.add(id + ":" + amount), "RES-7", 99.5).result.approved());
        assertEquals(List.of("RES-7:99.5"), calls);
    }

    @Test
    void providerRefusalIsReportedAsDeclined() {
        assertFalse(pay((id, amount) -> false, "RES-7", 99.5).result.approved());
    }

    @Test
    void missingReservationIdIsAnInvalidRequest() {
        RecordingPresenter presenter = pay(payFlow(), " ", 10.0);
        assertNull(presenter.result);
        assertNotNull(presenter.invalidReason);
    }
}
