package hotel.notification;

import java.util.ArrayList;
import java.util.List;
import hotel.model.Reservation;

public class ReservationEventPublisher {
    private final List<ReservationObserver> observers = new ArrayList<>();

    public void addObserver(ReservationObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(ReservationObserver observer) {
        observers.remove(observer);
    }

    public void notifyObservers(Reservation reservation, String event) {
        System.out.println("Publishing event: " + event);
        for (ReservationObserver observer : observers) {
            observer.update(reservation, event);
        }
    }
}
