package org.nightking.hotelmanagement.entity.lifecycle;

import org.nightking.hotelmanagement.entity.Reservation;

public interface ReservationState {
    void confirm(Reservation reservation);
    void checkIn(Reservation reservation);
    void checkOut(Reservation reservation);
    void cancel(Reservation reservation);
    String getName();

    /** Whether a reservation in this state keeps its room booked for its dates. */
    default boolean holdsRoom() { return false; }

    /** Rebuilds a state from its stored name. */
    static ReservationState fromName(String name) {
        return switch (name) {
            case "PENDING" -> new PendingState();
            case "CONFIRMED" -> new ConfirmedState();
            case "CHECKED_IN" -> new CheckedInState();
            case "COMPLETED" -> new CompletedState();
            case "CANCELLED" -> new CancelledState();
            default -> throw new IllegalArgumentException("Unknown reservation state: " + name);
        };
    }
}
