package org.nightking.hotelmanagement.infrastructure;

import org.nightking.hotelmanagement.entity.Reservation;
import org.nightking.hotelmanagement.gateway.ReservationObserver;

public class AuditLogObserver implements ReservationObserver {
    @Override
    public void update(Reservation reservation, String event) {
        System.out.println("[AUDIT] Reservation " + reservation.getId()
                + " changed: " + event + " (state=" + reservation.getState().getName() + ")");
    }
}
