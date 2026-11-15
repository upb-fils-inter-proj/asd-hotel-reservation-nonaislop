# MQTT broker for the messages between the hotel management, shop and payment services.
FROM eclipse-mosquitto:2
COPY mqtt-broker/mosquitto.conf mqtt-broker/acl /mosquitto/config/
COPY mqtt-broker/start.sh /usr/local/bin/start-broker.sh
RUN chmod 0755 /usr/local/bin/start-broker.sh && chmod 0644 /mosquitto/config/acl /mosquitto/config/mosquitto.conf
EXPOSE 1883
ENTRYPOINT ["/usr/local/bin/start-broker.sh"]
