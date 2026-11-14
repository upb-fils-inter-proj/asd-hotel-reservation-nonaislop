package hotel.state;

import hotel.model.Reservation;

public class PendingState implements ReservationState {
    public void confirm(Reservation r) {
        r.setState(new ConfirmedState());
        System.out.println("Reservation confirmed.");
    }
    public void checkIn(Reservation r) { reject("check in"); }
    public void checkOut(Reservation r) { reject("check out"); }
    public void cancel(Reservation r) {
        r.setState(new CancelledState());
        System.out.println("Pending reservation cancelled.");
    }
    public String getName() { return "PENDING"; }
    private void reject(String action) {
        System.out.println("Cannot " + action + " while reservation is pending.");
    }
}
