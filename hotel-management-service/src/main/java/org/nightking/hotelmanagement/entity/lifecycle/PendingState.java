package org.nightking.hotelmanagement.entity.lifecycle;

import org.nightking.hotelmanagement.entity.Reservation;

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
    /** A pending reservation holds its room while the guest is paying for it. */
    @Override public boolean holdsRoom() { return true; }
    private void reject(String action) {
        System.out.println("Cannot " + action + " while reservation is pending.");
    }
}
