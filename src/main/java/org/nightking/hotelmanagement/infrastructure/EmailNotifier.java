package org.nightking.hotelmanagement.infrastructure;

import org.nightking.hotelmanagement.entity.Reservation;
import org.nightking.hotelmanagement.gateway.ReservationObserver;

public class EmailNotifier implements ReservationObserver {
    @Override
    public void update(Reservation reservation, String event) {
        System.out.println("[EMAIL] To " + reservation.getGuest().getEmail()
                + ": reservation " + reservation.getId() + " " + event);
    }
}
