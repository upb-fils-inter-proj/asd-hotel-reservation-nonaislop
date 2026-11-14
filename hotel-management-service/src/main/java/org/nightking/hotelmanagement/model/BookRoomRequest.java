package org.nightking.hotelmanagement.model;

import java.time.LocalDate;

/** A booking is always made by a registered guest account. */
public record BookRoomRequest(String guestId,
                              String roomNumber,
                              LocalDate checkIn,
                              LocalDate checkOut,
                              boolean breakfast,
                              boolean spa,
                              boolean airportTransfer) {
}
