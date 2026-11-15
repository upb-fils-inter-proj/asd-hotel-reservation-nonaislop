package org.nightking.payment.infrastructure.messaging;

import java.util.function.UnaryOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Answers MQTT 5 requests: for each request on the topic, runs the handler on its JSON payload and publishes the
 * resulting JSON to the request's response topic, with the same correlation id so the caller can match it.
 */
public class MqttResponder {
    private static final Logger log = LoggerFactory.getLogger(MqttResponder.class);

    private final MqttTransport transport;

    public MqttResponder(MqttTransport transport) {
        this.transport = transport;
    }

    public void serve(String topicFilter, UnaryOperator<String> handler) {
        transport.subscribe(topicFilter, request -> {
            if (request.responseTopic() == null) {
                log.warn("Ignoring a request on {} without a response topic.", request.topic());
                return;
            }
            String reply = handler.apply(request.payload());
            transport.publish(new MqttEnvelope(request.responseTopic(), reply, null, request.correlationId()));
        });
    }
}
