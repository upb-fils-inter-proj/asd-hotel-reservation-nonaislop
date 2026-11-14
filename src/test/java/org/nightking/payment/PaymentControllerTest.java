package org.nightking.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.nightking.payment.controller.PaymentController;
import org.nightking.payment.infrastructure.PayFlowGateway;
import org.nightking.payment.infrastructure.PayFlowGatewayAdapter;
import org.nightking.payment.model.PaymentRequest;
import org.nightking.payment.model.PaymentResponse;
import org.nightking.payment.usecase.ProcessPaymentInteractor;

class PaymentControllerTest {
    private final PaymentController payFlow = new PaymentController(
            new ProcessPaymentInteractor(new PayFlowGatewayAdapter(new PayFlowGateway())));

    @Test
    void approvesPositiveAmount() {
        PaymentResponse response = payFlow.processPayment(new PaymentRequest("RES-1", 450.0));
        assertTrue(response.approved());
        assertEquals("RES-1", response.reservationId());
        assertEquals(450.0, response.amount(), 1e-9);
    }

    @Test
    void declinesZeroAmount() {
        assertFalse(payFlow.processPayment(new PaymentRequest("RES-1", 0.0)).approved());
    }

    @Test
    void passesReservationAndAmountToTheProvider() {
        List<String> calls = new ArrayList<>();
        PaymentController payment = new PaymentController(new ProcessPaymentInteractor(
                (reservationId, amount) -> calls.add(reservationId + ":" + amount)));

        assertTrue(payment.processPayment(new PaymentRequest("RES-7", 99.5)).approved());
        assertEquals(List.of("RES-7:99.5"), calls);
    }

    @Test
    void providerRefusalIsReportedAsDeclined() {
        PaymentController payment = new PaymentController(
                new ProcessPaymentInteractor((reservationId, amount) -> false));
        assertFalse(payment.processPayment(new PaymentRequest("RES-7", 99.5)).approved());
    }
}
