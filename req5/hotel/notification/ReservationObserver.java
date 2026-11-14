package hotel.notification;

import hotel.model.Reservation;

public interface ReservationObserver {
    void update(Reservation reservation, String event);
}
