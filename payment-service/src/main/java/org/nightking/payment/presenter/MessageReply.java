package org.nightking.payment.presenter;

/**
 * Reply envelope sent back over MQTT: status OK with the body, or INVALID_REQUEST, NOT_FOUND, REJECTED or ERROR
 * with an error message. The MQTT counterpart of an HTTP status code and body.
 */
public record MessageReply(String status, Object body, String error) {

    public static MessageReply ok(Object body) {
        return new MessageReply("OK", body, null);
    }

    public static MessageReply invalidRequest(String error) {
        return new MessageReply("INVALID_REQUEST", null, error);
    }

    public static MessageReply notFound(String error) {
        return new MessageReply("NOT_FOUND", null, error);
    }

    public static MessageReply rejected(String error) {
        return new MessageReply("REJECTED", null, error);
    }

    public static MessageReply error(String error) {
        return new MessageReply("ERROR", null, error);
    }
}
