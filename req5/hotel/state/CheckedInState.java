package hotel.state;

import hotel.model.Reservation;

public class CheckedInState implements ReservationState {
    public void confirm(Reservation r) { System.out.println("Reservation is already confirmed."); }
    public void checkIn(Reservation r) { System.out.println("Guest is already checked in."); }
    public void checkOut(Reservation r) {
        r.setState(new CompletedState());
        System.out.println("Guest checked out. Stay completed.");
    }
    public void cancel(Reservation r) { System.out.println("Cannot cancel after check-in."); }
    public String getName() { return "CHECKED_IN"; }
}
