package org.nightking.state;

import org.nightking.model.Reservation;

public class CompletedState implements ReservationState {
    public void confirm(Reservation r) { reject(); }
    public void checkIn(Reservation r) { reject(); }
    public void checkOut(Reservation r) { System.out.println("Reservation is already completed."); }
    public void cancel(Reservation r) { reject(); }
    public String getName() { return "COMPLETED"; }
    private void reject() { System.out.println("No changes allowed: reservation is completed."); }
}
