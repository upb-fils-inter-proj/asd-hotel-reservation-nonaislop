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
                          String state) {
}
