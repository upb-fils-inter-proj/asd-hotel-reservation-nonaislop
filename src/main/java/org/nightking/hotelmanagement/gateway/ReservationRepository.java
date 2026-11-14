package org.nightking.hotelmanagement.gateway;

import java.util.Optional;
import org.nightking.hotelmanagement.entity.Reservation;

public interface ReservationRepository {
    String nextId();
    Optional<Reservation> findById(String reservationId);
    void save(Reservation reservation);
}
