package org.nightking.notification;

import org.nightking.model.Reservation;

public class EmailNotifier implements ReservationObserver {
    @Override
    public void update(Reservation reservation, String event) {
        System.out.println("[EMAIL] To " + reservation.getGuest().getEmail()
                + ": reservation " + reservation.getId() + " " + event);
    }
}
