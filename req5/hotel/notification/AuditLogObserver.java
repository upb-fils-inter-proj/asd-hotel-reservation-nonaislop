package hotel.notification;

import hotel.model.Reservation;

public class AuditLogObserver implements ReservationObserver {
    @Override
    public void update(Reservation reservation, String event) {
        System.out.println("[AUDIT] Reservation " + reservation.getId()
                + " changed: " + event + " (state=" + reservation.getState().getName() + ")");
    }
}
