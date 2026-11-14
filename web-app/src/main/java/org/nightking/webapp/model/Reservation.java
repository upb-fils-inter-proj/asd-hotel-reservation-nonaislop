package org.nightking.webapp.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Reservation(String reservationId,
                          String guestId,
                          String guestName,
                          String roomNumber,
                          LocalDate checkIn,
                          LocalDate checkOut,
                          BigDecimal totalPrice,
                          String currency,
                          String state,
                          String paymentReference,
                          String paidWith) {

    /** Guests can cancel a paid reservation until they check in. */
    public boolean isCancellable() {
        return "CONFIRMED".equals(state);
    }

    /** A booking attempt whose card was declined: it was never paid, so the guest doesn't need to see it. */
    public boolean wasNeverPaid() {
        return "CANCELLED".equals(state) && paymentReference == null;
    }
}
