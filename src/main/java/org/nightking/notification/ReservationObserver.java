package org.nightking.notification;

import org.nightking.model.Reservation;

public interface ReservationObserver {
    void update(Reservation reservation, String event);
}
