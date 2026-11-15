package org.nightking.hotelmanagement.infrastructure.messaging;

import java.util.function.Consumer;

/** Sends and receives MQTT messages. {@link PahoMqttTransport} talks to the broker; tests use an in-memory one. */
public interface MqttTransport {

    /** Publishes a message with QoS 1 (at least once). Throws if the broker cannot be reached. */
    void publish(MqttEnvelope message);

    /** Calls the handler for every message on the topic filter; the subscription survives reconnects. */
    void subscribe(String topicFilter, Consumer<MqttEnvelope> handler);

    /** MQTT topic matching, with + and # wildcards and $share/group/ shared subscriptions. */
    static boolean matches(String topicFilter, String topic) {
        String filter = topicFilter;
        if (filter.startsWith("$share/")) {
            filter = filter.substring(filter.indexOf('/', "$share/".length()) + 1);
        }
        String[] levels = filter.split("/", -1);
        String[] topicLevels = topic.split("/", -1);
        for (int i = 0; i < levels.length; i++) {
            if (levels[i].equals("#")) return true;
            if (i >= topicLevels.length) return false;
            if (!levels[i].equals("+") && !levels[i].equals(topicLevels[i])) return false;
        }
        return levels.length == topicLevels.length;
    }
}
