package org.nightking.hotelmanagement.infrastructure.messaging;

/** The MQTT topics this service talks on; the broker's ACL (mqtt-broker/acl) allows exactly these. */
public final class MqttTopics {
    /** Requests answered by the shop service. */
    public static final String SHOP_QUOTE_REQUEST = "shop/quotes/request";
    /** Requests answered by the payment service. */
    public static final String PAYMENT_CHARGE_REQUEST = "payment/charges/request";
    public static final String PAYMENT_REFUND_REQUEST = "payment/refunds/request";
    /** Replies come back here: one topic per running instance, named in each request. */
    public static final String REPLIES_PREFIX = "hotel-management/replies/";
    /** Reservation events, e.g. hotel/reservations/confirmed. */
    public static final String RESERVATION_EVENTS_PREFIX = "hotel/reservations/";

    private MqttTopics() {
    }
}
