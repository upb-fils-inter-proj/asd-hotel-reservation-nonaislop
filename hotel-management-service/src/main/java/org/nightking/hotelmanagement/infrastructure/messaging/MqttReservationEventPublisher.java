package org.nightking.hotelmanagement.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import org.nightking.hotelmanagement.entity.Reservation;
import org.nightking.hotelmanagement.gateway.ReservationObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Observer that announces reservation changes on MQTT, e.g. hotel/reservations/confirmed, for any service that
 * wants to react (notifications, reporting, housekeeping). Carries no card or contact details.
 * A broker outage is logged and never fails the booking itself.
 */
public class MqttReservationEventPublisher implements ReservationObserver {
    private static final Logger log = LoggerFactory.getLogger(MqttReservationEventPublisher.class);

    private final MqttTransport transport;
    private final ObjectMapper json;
    private final Clock clock;

    public MqttReservationEventPublisher(MqttTransport transport, ObjectMapper json, Clock clock) {
        this.transport = transport;
        this.json = json;
        this.clock = clock;
    }

    @Override
    public void update(Reservation reservation, String event) {
        String topic = MqttTopics.RESERVATION_EVENTS_PREFIX + event.toLowerCase().replace('_', '-');
        ReservationEvent payload = new ReservationEvent(event, reservation.getId(), reservation.getGuest().getId(),
                reservation.getRoom().getRoomNumber(), reservation.getCheckInDate(), reservation.getCheckOutDate(),
                reservation.getTotalPrice(), reservation.getState().getName(), Instant.now(clock));
        try {
            transport.publish(MqttEnvelope.event(topic, json.writeValueAsString(payload)));
        } catch (JsonProcessingException | RuntimeException e) {
            log.warn("Could not publish {} for reservation {}: {}", topic, reservation.getId(), e.getMessage());
        }
    }

    /** Wire format of the reservation events. */
    record ReservationEvent(String event, String reservationId, String guestId, String roomNumber,
                            LocalDate checkIn, LocalDate checkOut, double totalPrice, String state,
                            Instant occurredAt) {}
}
