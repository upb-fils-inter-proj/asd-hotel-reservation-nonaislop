package org.nightking.state;

import org.nightking.model.Reservation;

public class CancelledState implements ReservationState {
    public void confirm(Reservation r) { reject(); }
    public void checkIn(Reservation r) { reject(); }
    public void checkOut(Reservation r) { reject(); }
    public void cancel(Reservation r) { System.out.println("Reservation is already cancelled."); }
    public String getName() { return "CANCELLED"; }
    private void reject() { System.out.println("No actions allowed: reservation is cancelled."); }
}
