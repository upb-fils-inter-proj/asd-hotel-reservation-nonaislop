package org.nightking.hotelmanagement.gateway;

import org.nightking.hotelmanagement.entity.Reservation;

public interface ReservationObserver {
    void update(Reservation reservation, String event);
}
