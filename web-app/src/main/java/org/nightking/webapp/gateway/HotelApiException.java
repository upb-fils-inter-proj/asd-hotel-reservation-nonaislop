package org.nightking.webapp.gateway;

/** The hotel management service refused a request; the message is safe to show to the user. */
public class HotelApiException extends RuntimeException {
    public HotelApiException(String message) {
        super(message);
    }
}
