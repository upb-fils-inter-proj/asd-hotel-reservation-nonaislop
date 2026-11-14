package org.nightking.hotelmanagement.model;

import java.time.LocalDate;

public record BookRoomRequest(GuestDetails guest,
                              String roomNumber,
                              LocalDate checkIn,
                              LocalDate checkOut,
                              boolean breakfast,
                              boolean spa,
                              boolean airportTransfer) {
}
