package org.nightking.hotelmanagement.gateway;

/**
 * Thrown by {@link ReservationRepository#save} when another reservation already holds the room for
 * one of the nights. This is the last line of defence against double bookings: it also catches two
 * bookings for the same nights that arrive at the same moment.
 */
public class RoomUnavailableException extends RuntimeException {
    public RoomUnavailableException(String roomNumber) {
        super("Room " + roomNumber + " is already booked for those dates.");
    }
}
