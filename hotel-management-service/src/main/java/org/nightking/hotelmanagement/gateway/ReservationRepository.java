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
    void save(Reservation reservation);
}
