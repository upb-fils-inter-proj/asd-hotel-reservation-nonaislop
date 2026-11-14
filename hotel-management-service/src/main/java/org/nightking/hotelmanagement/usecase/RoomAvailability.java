package org.nightking.hotelmanagement.usecase;

import java.time.LocalDate;
import org.nightking.hotelmanagement.gateway.ReservationRepository;

/** Whether a room is free for a stay: no reservation that holds the room overlaps the dates. */
public class RoomAvailability {
    private final ReservationRepository reservations;

    public RoomAvailability(ReservationRepository reservations) {
        this.reservations = reservations;
    }

    public boolean isFree(String roomNumber, LocalDate checkIn, LocalDate checkOut) {
        return reservations.findByRoomNumber(roomNumber).stream()
                .noneMatch(existing -> existing.blocks(checkIn, checkOut));
    }
}
