package org.nightking.hotelmanagement.model;

import java.time.LocalDate;
import org.nightking.hotelmanagement.entity.Reservation;

/** What callers see of a reservation. Entities never leave the service. */
public record ReservationResponse(String reservationId,
                                  String guestName,
                                  String roomNumber,
                                  LocalDate checkIn,
                                  LocalDate checkOut,
                                  double totalPrice,
                                  String state) {

    public static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getGuest().getName(),
                reservation.getRoom().getRoomNumber(),
                reservation.getCheckInDate(),
                reservation.getCheckOutDate(),
                reservation.getTotalPrice(),
                reservation.getState().getName());
    }
}
