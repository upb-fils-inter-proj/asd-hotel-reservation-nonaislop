package org.nightking.model;

import java.time.LocalDate;
import org.nightking.state.PendingState;
import org.nightking.state.ReservationState;

public class Reservation {
    private final String id;
    private final Guest guest;
    private final Room room;
    private final LocalDate checkInDate;
    private final LocalDate checkOutDate;
    private final double totalPrice;
    private ReservationState state;

    public Reservation(String id, Guest guest, Room room, LocalDate checkInDate,
                       LocalDate checkOutDate, double totalPrice) {
        this.id = id;
        this.guest = guest;
        this.room = room;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.totalPrice = totalPrice;
        this.state = new PendingState();
    }

    public String getId() { return id; }
    public Guest getGuest() { return guest; }
    public Room getRoom() { return room; }
    public LocalDate getCheckInDate() { return checkInDate; }
    public LocalDate getCheckOutDate() { return checkOutDate; }
    public double getTotalPrice() { return totalPrice; }
    public ReservationState getState() { return state; }
    public void setState(ReservationState state) { this.state = state; }

    public void confirm() { state.confirm(this); }
    public void checkIn() { state.checkIn(this); }
    public void checkOut() { state.checkOut(this); }
    public void cancel() { state.cancel(this); }
}
