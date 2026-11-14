package org.nightking.webapp.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** The price of a stay, shown to the guest before paying. */
public record BookingQuote(String roomNumber, String roomType, LocalDate checkIn, LocalDate checkOut, int nights,
                           BigDecimal total, String currency, String breakdown) {
}
