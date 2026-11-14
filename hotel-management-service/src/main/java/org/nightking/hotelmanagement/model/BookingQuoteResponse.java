package org.nightking.hotelmanagement.model;

import java.time.LocalDate;

public record BookingQuoteResponse(String roomNumber,
                                   String roomType,
                                   LocalDate checkIn,
                                   LocalDate checkOut,
                                   int nights,
                                   double total,
                                   String breakdown) {
}
