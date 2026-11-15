package org.nightking.hotelmanagement.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * The reply envelope the shop and payment services send back: status OK with a body, or another status
 * (INVALID_REQUEST, NOT_FOUND, REJECTED, ERROR) with an error message.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MqttReply(String status, JsonNode body, String error) {

    public boolean ok() {
        return "OK".equals(status);
    }
}
