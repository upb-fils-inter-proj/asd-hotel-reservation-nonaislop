package org.nightking.payment.infrastructure.messaging;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** A broker in a box for tests: delivers every published message straight to the matching subscribers. */
class InMemoryMqttTransport implements MqttTransport {
    final List<MqttEnvelope> published = new CopyOnWriteArrayList<>();
    private final Map<String, Consumer<MqttEnvelope>> handlers = new ConcurrentHashMap<>();

    @Override
    public void publish(MqttEnvelope message) {
        published.add(message);
        handlers.forEach((filter, handler) -> {
            if (MqttTransport.matches(filter, message.topic())) handler.accept(message);
        });
    }

    @Override
    public void subscribe(String topicFilter, Consumer<MqttEnvelope> handler) {
        handlers.put(topicFilter, handler);
    }

    /** Plays the hotel management service: sends a request and returns the JSON reply it got back. */
    String request(String topic, String json) {
        String replyTopic = "hotel-management/replies/test";
        String correlationId = java.util.UUID.randomUUID().toString();
        publish(new MqttEnvelope(topic, json, replyTopic, correlationId));
        return published.stream()
                .filter(m -> m.topic().equals(replyTopic) && correlationId.equals(m.correlationId()))
                .findFirst()
                .map(MqttEnvelope::payload)
                .orElseThrow(() -> new AssertionError("no reply on " + replyTopic));
    }
}
