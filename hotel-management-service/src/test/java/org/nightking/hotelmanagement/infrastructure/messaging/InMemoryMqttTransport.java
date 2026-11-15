package org.nightking.hotelmanagement.infrastructure.messaging;

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

    /** Plays a service: answers every request on the topic with the given reply JSON. */
    void respond(String topicFilter, String replyJson) {
        subscribe(topicFilter, request ->
                publish(new MqttEnvelope(request.responseTopic(), replyJson, null, request.correlationId())));
    }
}
