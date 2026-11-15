package org.nightking.hotelmanagement.infrastructure.messaging;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MQTT 5 connection to the broker (Eclipse Paho). Connects in the background and keeps retrying until the
 * broker is up, then lets Paho reconnect; subscriptions are made again after every (re)connect.
 * Messages are handled on virtual threads, never on Paho's own callback thread.
 */
public class PahoMqttTransport implements MqttTransport, AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(PahoMqttTransport.class);
    private static final int QOS = 1;

    private final MqttClient client;
    private final MqttConnectionOptions options;
    private final Map<String, Consumer<MqttEnvelope>> handlers = new ConcurrentHashMap<>();
    private final ExecutorService workers = Executors.newVirtualThreadPerTaskExecutor();
    private volatile boolean closed;

    public PahoMqttTransport(String serverUri, String clientId, String username, String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "messaging.mqtt.password is empty: provide the mqtt_hotel_password secret (see README).");
        }
        try {
            this.client = new MqttClient(serverUri, clientId, new MemoryPersistence());
        } catch (MqttException e) {
            throw new IllegalStateException("Invalid MQTT broker address " + serverUri, e);
        }
        this.options = new MqttConnectionOptions();
        options.setUserName(username);
        options.setPassword(password.getBytes(StandardCharsets.UTF_8));
        options.setCleanStart(true);
        options.setAutomaticReconnect(true);
        options.setAutomaticReconnectDelay(1, 30);
        options.setConnectionTimeout(10);
        options.setKeepAliveInterval(30);
        client.setCallback(new Callback());
    }

    /** Starts connecting in the background, so the service starts even when the broker is not up yet. */
    public void start() {
        Thread.ofVirtual().name("mqtt-connect").start(() -> {
            int attempt = 0;
            while (!closed && !client.isConnected()) {
                try {
                    client.connect(options);
                } catch (MqttException e) {
                    if (attempt++ % 10 == 0) {
                        log.warn("MQTT broker {} not reachable yet ({}); retrying.", client.getServerURI(), e.getMessage());
                    }
                    sleep(2_000);
                }
            }
        });
    }

    public boolean isConnected() {
        return client.isConnected();
    }

    @Override
    public void publish(MqttEnvelope message) {
        MqttProperties properties = new MqttProperties();
        properties.setContentType("application/json");
        if (message.responseTopic() != null) properties.setResponseTopic(message.responseTopic());
        if (message.correlationId() != null) {
            properties.setCorrelationData(message.correlationId().getBytes(StandardCharsets.UTF_8));
        }
        MqttMessage mqttMessage = new MqttMessage(message.payload().getBytes(StandardCharsets.UTF_8));
        mqttMessage.setQos(QOS);
        mqttMessage.setProperties(properties);
        try {
            client.publish(message.topic(), mqttMessage);
        } catch (MqttException e) {
            throw new IllegalStateException("Could not publish to MQTT topic " + message.topic() + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void subscribe(String topicFilter, Consumer<MqttEnvelope> handler) {
        handlers.put(topicFilter, handler);
        if (client.isConnected()) {
            subscribeOnBroker(topicFilter);
        }
    }

    private void subscribeOnBroker(String topicFilter) {
        try {
            client.subscribe(topicFilter, QOS);
            log.info("Subscribed to MQTT topic {}", topicFilter);
        } catch (MqttException e) {
            log.warn("Could not subscribe to {} ({}); will retry after the next reconnect.", topicFilter, e.getMessage());
        }
    }

    @Override
    public void close() {
        closed = true;
        try {
            if (client.isConnected()) client.disconnect();
            client.close();
        } catch (MqttException e) {
            log.debug("Error while closing the MQTT client", e);
        }
        workers.shutdown();
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private class Callback implements MqttCallback {
        @Override
        public void connectComplete(boolean reconnect, String serverURI) {
            log.info("{} MQTT broker {}", reconnect ? "Reconnected to" : "Connected to", serverURI);
            // Clean start: the broker forgot our subscriptions, so make them again (off Paho's callback thread).
            workers.submit(() -> handlers.keySet().forEach(PahoMqttTransport.this::subscribeOnBroker));
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) {
            MqttProperties properties = message.getProperties();
            byte[] correlation = properties == null ? null : properties.getCorrelationData();
            MqttEnvelope envelope = new MqttEnvelope(topic,
                    new String(message.getPayload(), StandardCharsets.UTF_8),
                    properties == null ? null : properties.getResponseTopic(),
                    correlation == null ? null : new String(correlation, StandardCharsets.UTF_8));
            handlers.forEach((filter, handler) -> {
                if (MqttTransport.matches(filter, topic)) {
                    workers.submit(() -> {
                        try {
                            handler.accept(envelope);
                        } catch (RuntimeException e) {
                            log.error("Handler for MQTT topic {} failed", topic, e);
                        }
                    });
                }
            });
        }

        @Override
        public void disconnected(MqttDisconnectResponse response) {
            if (!closed) log.warn("Disconnected from the MQTT broker: {}", response.getReasonString());
        }

        @Override
        public void mqttErrorOccurred(MqttException exception) {
            log.warn("MQTT error: {}", exception.getMessage());
        }

        @Override
        public void deliveryComplete(IMqttToken token) {
        }

        @Override
        public void authPacketArrived(int reasonCode, MqttProperties properties) {
        }
    }
}
