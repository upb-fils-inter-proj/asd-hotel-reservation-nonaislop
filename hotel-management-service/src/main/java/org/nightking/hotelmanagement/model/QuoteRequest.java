package org.nightking.hotelmanagement.model;

import java.time.LocalDate;

/** Price a stay in a room before booking it. */
public record QuoteRequest(String roomNumber,
                           LocalDate checkIn,
                           LocalDate checkOut,
                           boolean breakfast,
                           boolean spa,
                           boolean airportTransfer) {
}
