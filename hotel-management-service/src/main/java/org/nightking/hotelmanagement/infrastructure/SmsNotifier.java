package org.nightking.hotelmanagement.infrastructure;

import org.nightking.hotelmanagement.entity.Reservation;
import org.nightking.hotelmanagement.gateway.ReservationObserver;

public class SmsNotifier implements ReservationObserver {
    @Override
    public void update(Reservation reservation, String event) {
        System.out.println("[SMS] To " + reservation.getGuest().getPhone()
                + ": reservation " + reservation.getId() + " " + event);
    }
}
