package org.nightking.payment.infrastructure;

/*
 * Simulates a third-party/legacy gateway with a different method signature.
 * It is deliberately not compatible with PaymentProcessor.
 */
public class PayFlowGateway {
    public boolean makeCharge(String bookingReference, int amountInCents) {
        System.out.println("[PayFlow] Charging " + amountInCents
                + " cents for booking " + bookingReference);
        return amountInCents > 0;
    }
}
