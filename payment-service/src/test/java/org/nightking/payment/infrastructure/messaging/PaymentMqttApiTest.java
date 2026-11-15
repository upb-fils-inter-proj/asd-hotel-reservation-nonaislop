package org.nightking.payment.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.convention.TestBean;

/** The payment service's MQTT API (charges and refunds) on a real (H2) database, with an in-memory broker. */
@SpringBootTest(properties = "payment_api_password=test-secret")
@ActiveProfiles("test")
class PaymentMqttApiTest {
    private static final String CHARGES = "payment/charges/request";
    private static final String REFUNDS = "payment/refunds/request";

    /** Replaces the real broker connection with an in-memory broker. */
    @TestBean(name = "mqttTransport")
    private MqttTransport mqttTransport;

    static MqttTransport mqttTransport() {
        return new InMemoryMqttTransport();
    }

    private InMemoryMqttTransport broker() {
        return (InMemoryMqttTransport) mqttTransport;
    }

    @Autowired
    private ObjectMapper json;

    private static String charge(String reservationId, String cardNumber) {
        return """
                {"reservationId": "%s", "amount": 450.0,
                 "card": {"holderName": "Alex Morgan", "number": "%s", "expiryMonth": 12, "expiryYear": 2040, "cvc": "123"}}
                """.formatted(reservationId, cardNumber);
    }

    private JsonNode send(String topic, String body) throws Exception {
        return json.readTree(broker().request(topic, body));
    }

    @Test
    void chargesAndRefundsACard() throws Exception {
        JsonNode charged = send(CHARGES, charge("RES-MQTT-1", "4242424242424242"));
        assertEquals("OK", charged.get("status").asText());
        assertEquals("APPROVED", charged.at("/body/status").asText());
        assertEquals("Visa •••• 4242", charged.at("/body/card").asText());
        String transactionId = charged.at("/body/transactionId").asText();
        assertTrue(transactionId.startsWith("PAY-"));

        JsonNode refunded = send(REFUNDS, "{\"transactionId\": \"" + transactionId + "\"}");
        assertEquals("OK", refunded.get("status").asText());
        assertEquals("REFUNDED", refunded.at("/body/status").asText());

        JsonNode again = send(REFUNDS, "{\"transactionId\": \"" + transactionId + "\"}");
        assertEquals("REJECTED", again.get("status").asText());
    }

    @Test
    void aDeclineIsAnAnswerNotAnError() throws Exception {
        JsonNode declined = send(CHARGES, charge("RES-MQTT-2", "4000000000000002"));
        assertEquals("OK", declined.get("status").asText());
        assertEquals("DECLINED", declined.at("/body/status").asText());
        assertFalse(declined.at("/body/declineReason").asText().isBlank());
    }

    @Test
    void aMistypedCardIsDeclinedAndAMissingCardIsAnInvalidRequest() throws Exception {
        JsonNode mistyped = send(CHARGES, charge("RES-MQTT-3", "1234"));
        assertEquals("OK", mistyped.get("status").asText());
        assertEquals("DECLINED", mistyped.at("/body/status").asText());

        JsonNode noCard = send(CHARGES, "{\"reservationId\": \"RES-MQTT-3\", \"amount\": 10.0}");
        assertEquals("INVALID_REQUEST", noCard.get("status").asText());
        assertEquals("card details are required.", noCard.get("error").asText());
    }

    @Test
    void refundingAnUnknownPaymentIsNotFound() throws Exception {
        assertEquals("NOT_FOUND", send(REFUNDS, "{\"transactionId\": \"PAY-nope\"}").get("status").asText());
        assertEquals("INVALID_REQUEST", send(REFUNDS, "{}").get("status").asText());
    }

    @Test
    void theCardNumberNeverComesBack() throws Exception {
        String reply = broker().request(CHARGES, charge("RES-MQTT-4", "4242424242424242"));
        assertFalse(reply.contains("4242424242424242"));
        assertFalse(reply.contains("\"123\""));
    }
}
