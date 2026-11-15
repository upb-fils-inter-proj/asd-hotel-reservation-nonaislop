package org.nightking.shop.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.convention.TestBean;

/** The shop's MQTT API (shop/quotes/request) on a real (H2) database, with an in-memory broker. */
@SpringBootTest(properties = "shop_api_password=test-secret")
@ActiveProfiles("test")
class ShopMqttApiTest {
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

    @Test
    void quotesAStayOverMqtt() throws Exception {
        JsonNode reply = json.readTree(broker().request("shop/quotes/request",
                "{\"nightlyRate\": 100.0, \"nights\": 3, \"breakfast\": true, \"spa\": true, \"airportTransfer\": false}"));

        assertEquals("OK", reply.get("status").asText());
        assertEquals(450.0, reply.at("/body/total").asDouble(), 1e-9);
        assertEquals("EUR", reply.at("/body/currency").asText());
        assertTrue(reply.at("/body/breakdown").asText().contains("Breakfast"));
    }

    @Test
    void rejectsAnInvalidStay() throws Exception {
        JsonNode reply = json.readTree(broker().request("shop/quotes/request",
                "{\"nightlyRate\": 100.0, \"nights\": 0}"));
        assertEquals("INVALID_REQUEST", reply.get("status").asText());
        assertTrue(reply.get("error").asText().contains("nights"));
    }

    @Test
    void answersUnreadableRequestsInsteadOfStayingSilent() throws Exception {
        JsonNode reply = json.readTree(broker().request("shop/quotes/request", "not json"));
        assertEquals("INVALID_REQUEST", reply.get("status").asText());
    }
}
