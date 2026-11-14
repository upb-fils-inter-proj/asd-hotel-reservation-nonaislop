package hotel.notification;

import hotel.model.Reservation;

public class SmsNotifier implements ReservationObserver {
    @Override
    public void update(Reservation reservation, String event) {
        System.out.println("[SMS] To " + reservation.getGuest().getPhone()
                + ": reservation " + reservation.getId() + " " + event);
    }
}
