package org.nightking.hotelmanagement.entity;

import java.time.LocalDate;
import org.nightking.hotelmanagement.entity.lifecycle.PendingState;
import org.nightking.hotelmanagement.entity.lifecycle.ReservationState;

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

    /** True if this reservation keeps the room booked on any night between the given dates. */
    public boolean blocks(LocalDate from, LocalDate to) {
        return state.holdsRoom() && checkInDate.isBefore(to) && from.isBefore(checkOutDate);
    }

    public void confirm() { state.confirm(this); }
    public void checkIn() { state.checkIn(this); }
    public void checkOut() { state.checkOut(this); }
    public void cancel() { state.cancel(this); }
}
