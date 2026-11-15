package org.nightking.hotelmanagement.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Request/reply over MQTT 5: publishes a request that names this instance's reply topic and a fresh
 * correlation id, then waits (up to the timeout) for the reply carrying the same id.
 */
public class MqttRequester {
    private static final Logger log = LoggerFactory.getLogger(MqttRequester.class);

    private final MqttTransport transport;
    private final ObjectMapper json;
    private final String replyTopic;
    private final Duration timeout;
    private final Map<String, CompletableFuture<MqttReply>> pending = new ConcurrentHashMap<>();

    public MqttRequester(MqttTransport transport, ObjectMapper json, String replyTopic, Duration timeout) {
        this.transport = transport;
        this.json = json;
        this.replyTopic = replyTopic;
        this.timeout = timeout;
        transport.subscribe(replyTopic, this::onReply);
    }

    /** Sends the body as JSON to the topic and returns the reply. Throws if no reply arrives in time. */
    public MqttReply request(String topic, Object body) {
        String correlationId = UUID.randomUUID().toString();
        CompletableFuture<MqttReply> reply = new CompletableFuture<>();
        pending.put(correlationId, reply);
        try {
            transport.publish(new MqttEnvelope(topic, write(body), replyTopic, correlationId));
            return reply.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new IllegalStateException("No reply on MQTT topic " + topic + " within " + timeout.toSeconds()
                    + "s: is the service behind it running?", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for a reply on " + topic, e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Request on " + topic + " failed", e.getCause());
        } finally {
            pending.remove(correlationId);
        }
    }

    /** Reads the reply body as the given type. */
    public <T> T read(MqttReply reply, Class<T> type) {
        if (reply.body() == null || reply.body().isNull()) {
            throw new IllegalStateException("The reply has no body.");
        }
        try {
            return json.treeToValue(reply.body(), type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unreadable reply body: " + e.getOriginalMessage(), e);
        }
    }

    private void onReply(MqttEnvelope message) {
        CompletableFuture<MqttReply> waiting = message.correlationId() == null ? null : pending.get(message.correlationId());
        if (waiting == null) {
            log.warn("Ignoring a reply on {} that matches no open request (late or duplicate).", message.topic());
            return;
        }
        try {
            waiting.complete(json.readValue(message.payload(), MqttReply.class));
        } catch (JsonProcessingException e) {
            waiting.completeExceptionally(new IllegalStateException("Unreadable reply: " + e.getOriginalMessage(), e));
        }
    }

    private String write(Object body) {
        try {
            return json.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialise the request", e);
        }
    }
}
