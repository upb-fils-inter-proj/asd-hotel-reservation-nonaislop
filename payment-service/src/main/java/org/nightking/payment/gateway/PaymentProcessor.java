package org.nightking.payment.gateway;

import org.nightking.payment.entity.PaymentCard;

/** The card network the payment service charges through (implemented by an adapter). */
public interface PaymentProcessor {
    ChargeOutcome charge(String transactionId, String reservationId, double amount, PaymentCard card);

    boolean refund(String transactionId, double amount);

    record ChargeOutcome(boolean approved, String declineReason) {
        public static ChargeOutcome approvedCharge() { return new ChargeOutcome(true, null); }
        public static ChargeOutcome declined(String reason) { return new ChargeOutcome(false, reason); }
    }
}
