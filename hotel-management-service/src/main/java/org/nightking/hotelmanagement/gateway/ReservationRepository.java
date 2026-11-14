package org.nightking.hotelmanagement.gateway;

import java.util.List;
import java.util.Optional;
import org.nightking.hotelmanagement.entity.Reservation;

public interface ReservationRepository {
    String nextId();
    Optional<Reservation> findById(String reservationId);
    List<Reservation> findAll();
    List<Reservation> findByGuestId(String guestId);
    List<Reservation> findByRoomNumber(String roomNumber);

    /**
     * Saves the reservation. While its state holds the room, it owns the room for each of its nights.
     *
     * @throws RoomUnavailableException if another reservation already holds the room for one of them
     */
    void save(Reservation reservation);
}
