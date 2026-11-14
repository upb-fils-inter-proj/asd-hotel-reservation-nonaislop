package org.nightking.webapp.model;

import java.time.LocalDate;

public record BookingRequest(String guestId,
                             String roomNumber,
                             LocalDate checkIn,
                             LocalDate checkOut,
                             boolean breakfast,
                             boolean spa,
                             boolean airportTransfer) {
}
