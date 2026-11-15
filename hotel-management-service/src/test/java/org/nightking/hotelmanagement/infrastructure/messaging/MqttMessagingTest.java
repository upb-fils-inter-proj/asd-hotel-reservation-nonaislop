package org.nightking.hotelmanagement.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.nightking.hotelmanagement.entity.Guest;
import org.nightking.hotelmanagement.entity.Reservation;
import org.nightking.hotelmanagement.entity.Room;
import org.nightking.hotelmanagement.gateway.PaymentGateway;
import org.nightking.hotelmanagement.gateway.PaymentGateway.ChargeResult;
import org.nightking.hotelmanagement.gateway.ShopGateway.StayQuote;

/** The MQTT contract the hotel management service uses to talk to the shop and payment services. */
class MqttMessagingTest {
    private static final String REPLIES = MqttTopics.REPLIES_PREFIX + "hotel-management-test";
    private static final PaymentGateway.Card CARD =
            new PaymentGateway.Card("Alex Morgan", "4242424242424242", 12, 2030, "123");

    private final ObjectMapper json = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final InMemoryMqttTransport broker = new InMemoryMqttTransport();
    private final MqttRequester requester = new MqttRequester(broker, json, REPLIES, Duration.ofSeconds(2));

    private JsonNode lastRequestOn(String topic) throws Exception {
        MqttEnvelope request = broker.published.stream().filter(m -> m.topic().equals(topic)).findFirst().orElseThrow();
        assertEquals(REPLIES, request.responseTopic());
        assertNotNull(request.correlationId());
        return json.readTree(request.payload());
    }

    @Test
    void shopClientAsksForAQuoteAndReadsTheReply() throws Exception {
        broker.respond("$share/shop-service/shop/quotes/request",
                "{\"status\":\"OK\",\"body\":{\"total\":450.0,\"currency\":\"EUR\",\"breakdown\":\"Room stay + Breakfast\"}}");

        StayQuote quote = new ShopMqttClient(requester).quoteStay(100.0, 3, true, true, false);

        assertEquals(450.0, quote.total(), 1e-9);
        assertEquals("Room stay + Breakfast", quote.breakdown());
        JsonNode request = lastRequestOn(MqttTopics.SHOP_QUOTE_REQUEST);
        assertEquals(100.0, request.get("nightlyRate").asDouble());
        assertEquals(3, request.get("nights").asInt());
        assertTrue(request.get("breakfast").asBoolean());
        assertFalse(request.get("airportTransfer").asBoolean());
    }

    @Test
    void shopErrorsBecomeExceptions() {
        broker.respond(MqttTopics.SHOP_QUOTE_REQUEST, "{\"status\":\"INVALID_REQUEST\",\"error\":\"nights must be > 0\"}");
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> new ShopMqttClient(requester).quoteStay(100.0, 0, false, false, false));
        assertTrue(e.getMessage().contains("nights must be > 0"));
    }

    @Test
    void paymentClientSendsTheCardAndMapsApprovedAndDeclined() throws Exception {
        broker.respond(MqttTopics.PAYMENT_CHARGE_REQUEST, """
                {"status":"OK","body":{"transactionId":"PAY-1","reservationId":"RES-1000","amount":450.0,
                 "status":"APPROVED","declineReason":null,"card":"Visa •••• 4242"}}""");
        ChargeResult approved = new PaymentMqttClient(requester).charge("RES-1000", 450.0, CARD);
        assertTrue(approved.approved());
        assertEquals("PAY-1", approved.paymentReference());
        assertEquals("Visa •••• 4242", approved.paidWith());

        JsonNode request = lastRequestOn(MqttTopics.PAYMENT_CHARGE_REQUEST);
        assertEquals("RES-1000", request.get("reservationId").asText());
        assertEquals(450.0, request.get("amount").asDouble());
        assertEquals("4242424242424242", request.at("/card/number").asText());
        assertEquals(12, request.at("/card/expiryMonth").asInt());

        broker.respond(MqttTopics.PAYMENT_CHARGE_REQUEST,
                "{\"status\":\"OK\",\"body\":{\"transactionId\":\"PAY-2\",\"status\":\"DECLINED\",\"declineReason\":\"Insufficient funds.\"}}");
        ChargeResult declined = new PaymentMqttClient(requester).charge("RES-1001", 450.0, CARD);
        assertFalse(declined.approved());
        assertEquals("Insufficient funds.", declined.declineReason());
    }

    @Test
    void anInvalidCardIsADeclineWithTheReason() {
        broker.respond(MqttTopics.PAYMENT_CHARGE_REQUEST, "{\"status\":\"INVALID_REQUEST\",\"error\":\"Card number is invalid.\"}");
        ChargeResult result = new PaymentMqttClient(requester).charge("RES-1", 10.0, CARD);
        assertFalse(result.approved());
        assertNull(result.paymentReference());
        assertEquals("Card number is invalid.", result.declineReason());
    }

    @Test
    void refundsByPaymentReferenceAndFailsLoudlyWhenRefused() throws Exception {
        broker.respond(MqttTopics.PAYMENT_REFUND_REQUEST, "{\"status\":\"OK\",\"body\":{\"transactionId\":\"PAY-1\",\"status\":\"REFUNDED\"}}");
        new PaymentMqttClient(requester).refund("PAY-1");
        assertEquals("PAY-1", lastRequestOn(MqttTopics.PAYMENT_REFUND_REQUEST).get("transactionId").asText());

        broker.respond(MqttTopics.PAYMENT_REFUND_REQUEST, "{\"status\":\"NOT_FOUND\",\"error\":\"Payment PAY-9 not found.\"}");
        assertThrows(IllegalStateException.class, () -> new PaymentMqttClient(requester).refund("PAY-9"));
    }

    @Test
    void noReplyInTimeIsAnError() {
        MqttRequester impatient = new MqttRequester(broker, json, REPLIES, Duration.ofMillis(100));
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> new ShopMqttClient(impatient).quoteStay(100.0, 1, false, false, false));
        assertTrue(e.getMessage().contains("No reply"));
    }

    @Test
    void repliesWithAnUnknownCorrelationIdAreIgnored() {
        broker.publish(new MqttEnvelope(REPLIES, "{\"status\":\"OK\"}", null, "nobody-asked"));
        broker.publish(new MqttEnvelope(REPLIES, "{\"status\":\"OK\"}", null, null));
        // nothing to assert beyond "no exception": the stray replies are logged and dropped
    }

    @Test
    void reservationEventsArePublishedWithoutPaymentOrContactDetails() throws Exception {
        Clock clock = Clock.fixed(Instant.parse("2026-11-01T09:00:00Z"), ZoneOffset.UTC);
        Reservation reservation = new Reservation("RES-7", new Guest("G-1", "Alex Morgan", "alex@example.com", "+40 700"),
                new Room("101", "Standard", 80.0), LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 3), 160.0);
        reservation.recordPayment("PAY-7", "Visa •••• 4242");

        new MqttReservationEventPublisher(broker, json, clock).update(reservation, "CHECKED_IN");

        MqttEnvelope event = broker.published.getFirst();
        assertEquals("hotel/reservations/checked-in", event.topic());
        assertNull(event.responseTopic());
        JsonNode body = json.readTree(event.payload());
        assertEquals("RES-7", body.get("reservationId").asText());
        assertEquals("101", body.get("roomNumber").asText());
        assertEquals("2026-12-01", body.get("checkIn").asText());
        assertEquals("2026-11-01T09:00:00Z", body.get("occurredAt").asText());
        assertFalse(event.payload().contains("4242"));
        assertFalse(event.payload().contains("alex@example.com"));
    }

    @Test
    void aBrokerOutageNeverBreaksTheReservation() {
        MqttTransport down = new InMemoryMqttTransport() {
            @Override
            public void publish(MqttEnvelope message) {
                throw new IllegalStateException("broker down");
            }
        };
        Reservation reservation = new Reservation("RES-8", new Guest("G-1", "A", "a@b.c", "1"),
                new Room("101", "Standard", 80.0), LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 2), 80.0);
        new MqttReservationEventPublisher(down, json, Clock.systemUTC()).update(reservation, "CANCELLED");
    }

    @Test
    void topicFiltersFollowMqttRules() {
        assertTrue(MqttTransport.matches("shop/quotes/request", "shop/quotes/request"));
        assertTrue(MqttTransport.matches("$share/shop-service/shop/quotes/request", "shop/quotes/request"));
        assertTrue(MqttTransport.matches("hotel/reservations/#", "hotel/reservations/confirmed"));
        assertTrue(MqttTransport.matches("hotel/+/confirmed", "hotel/reservations/confirmed"));
        assertFalse(MqttTransport.matches("hotel/reservations", "hotel/reservations/confirmed"));
        assertFalse(MqttTransport.matches("hotel/reservations/confirmed", "hotel/reservations"));
        assertFalse(MqttTransport.matches("payment/charges/request", "payment/refunds/request"));
    }

    @Test
    void cardDetailsAreNeverPrinted() {
        assertFalse(CARD.toString().contains("4242424242424242"));
        assertFalse(CARD.toString().contains("123"));
    }

    @Test
    void transportRefusesToStartWithoutASecret() {
        assertThrows(IllegalStateException.class,
                () -> new PahoMqttTransport("tcp://localhost:1883", "test", "hotel-management", ""));
        assertThrows(IllegalStateException.class,
                () -> new PahoMqttTransport("tcp://localhost:1883", "test", "hotel-management", null));
    }
}
