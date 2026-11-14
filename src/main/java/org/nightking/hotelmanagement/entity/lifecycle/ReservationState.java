package org.nightking.hotelmanagement.entity.lifecycle;

import org.nightking.hotelmanagement.entity.Reservation;

public interface ReservationState {
    void confirm(Reservation reservation);
    void checkIn(Reservation reservation);
    void checkOut(Reservation reservation);
    void cancel(Reservation reservation);
    String getName();
}
