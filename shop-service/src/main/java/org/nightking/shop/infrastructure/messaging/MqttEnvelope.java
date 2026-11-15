package org.nightking.shop.infrastructure.messaging;

/**
 * One MQTT message: a JSON payload on a topic, plus the MQTT 5 request/response properties
 * (where to send the reply, and the id that ties the reply to its request). Both are null on replies.
 */
public record MqttEnvelope(String topic, String payload, String responseTopic, String correlationId) {
}
