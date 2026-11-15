package org.nightking.hotelmanagement.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.nightking.hotelmanagement.gateway.PaymentGateway;

/** Charges and refunds through the payment service, over MQTT (topics payment/charges/request and payment/refunds/request). */
public class PaymentMqttClient implements PaymentGateway {
    private final MqttRequester requester;

    public PaymentMqttClient(MqttRequester requester) {
        this.requester = requester;
    }

    @Override
    public ChargeResult charge(String reservationId, double amount, Card card) {
        MqttReply reply = requester.request(MqttTopics.PAYMENT_CHARGE_REQUEST,
                new ChargeRequest(reservationId, amount, new CardPayload(card.holderName(), card.number(),
                        card.expiryMonth(), card.expiryYear(), card.cvc())));
        if ("INVALID_REQUEST".equals(reply.status())) {
            // e.g. a mistyped card number: the card cannot be charged, so the booking is declined with that reason.
            return new ChargeResult(false, null, null, reply.error());
        }
        if (!reply.ok()) {
            throw new IllegalStateException("The payment service could not charge the card: " + reply.error());
        }
        PaymentResult result = requester.read(reply, PaymentResult.class);
        return new ChargeResult("APPROVED".equals(result.status()), result.transactionId(), result.card(),
                result.declineReason());
    }

    @Override
    public void refund(String paymentReference) {
        MqttReply reply = requester.request(MqttTopics.PAYMENT_REFUND_REQUEST, new RefundRequest(paymentReference));
        if (!reply.ok()) {
            throw new IllegalStateException("The payment service could not refund " + paymentReference + ": " + reply.error());
        }
    }

    /** Wire format of the payment messages, owned by this client so the services share no code. */
    record ChargeRequest(String reservationId, double amount, CardPayload card) {}

    record CardPayload(String holderName, String number, int expiryMonth, int expiryYear, String cvc) {
        @Override
        public String toString() { return "CardPayload[hidden]"; }
    }

    record RefundRequest(String transactionId) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PaymentResult(String transactionId, String status, String declineReason, String card) {}
}
