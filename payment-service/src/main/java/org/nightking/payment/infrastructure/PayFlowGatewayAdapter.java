package org.nightking.payment.infrastructure;

import org.nightking.payment.entity.PaymentCard;
import org.nightking.payment.gateway.PaymentProcessor;
import org.nightking.payment.infrastructure.PayFlowGateway.PayFlowResponse;

/** Adapter: makes the PayFlow network look like the PaymentProcessor the use cases expect. */
public class PayFlowGatewayAdapter implements PaymentProcessor {
    private final PayFlowGateway gateway;

    public PayFlowGatewayAdapter(PayFlowGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public ChargeOutcome charge(String transactionId, String reservationId, double amount, PaymentCard card) {
        String expiry = String.format("%02d/%02d", card.expiryMonth(), card.expiryYear() % 100);
        PayFlowResponse response = gateway.makeCharge(reservationId, transactionId, toCents(amount),
                card.number(), expiry, card.cvc());
        return response.isApproved()
                ? ChargeOutcome.approvedCharge()
                : ChargeOutcome.declined(describe(response.responseCode()));
    }

    @Override
    public boolean refund(String transactionId, double amount) {
        return gateway.refundCharge(transactionId, toCents(amount)).isApproved();
    }

    private static int toCents(double euros) {
        return (int) Math.round(euros * 100);
    }

    /** Turns PayFlow's response codes into messages a guest can act on. */
    private static String describe(String responseCode) {
        return switch (responseCode) {
            case PayFlowGateway.INSUFFICIENT_FUNDS -> "Insufficient funds.";
            case PayFlowGateway.EXPIRED_CARD -> "The card has expired.";
            case PayFlowGateway.INVALID_AMOUNT -> "The amount is not valid.";
            default -> "Your card was declined.";
        };
    }
}
