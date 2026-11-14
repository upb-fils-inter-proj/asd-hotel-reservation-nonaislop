package org.nightking.webapp.model;

import java.time.LocalDate;

public record QuoteRequest(String roomNumber, LocalDate checkIn, LocalDate checkOut,
                           boolean breakfast, boolean spa, boolean airportTransfer) {
}
