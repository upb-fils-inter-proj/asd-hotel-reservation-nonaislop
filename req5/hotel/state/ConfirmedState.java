package hotel.state;

import hotel.model.Reservation;

public class ConfirmedState implements ReservationState {
    public void confirm(Reservation r) { System.out.println("Reservation is already confirmed."); }
    public void checkIn(Reservation r) {
        r.setState(new CheckedInState());
        System.out.println("Guest checked in.");
    }
    public void checkOut(Reservation r) { reject("check out"); }
    public void cancel(Reservation r) {
        r.setState(new CancelledState());
        System.out.println("Confirmed reservation cancelled.");
    }
    public String getName() { return "CONFIRMED"; }
    private void reject(String action) {
        System.out.println("Cannot " + action + " before check-in.");
    }
}
