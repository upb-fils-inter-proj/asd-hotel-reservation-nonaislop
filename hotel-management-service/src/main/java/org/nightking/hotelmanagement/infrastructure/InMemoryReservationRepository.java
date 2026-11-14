package org.nightking.hotelmanagement.infrastructure;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.nightking.hotelmanagement.entity.Reservation;
import org.nightking.hotelmanagement.gateway.ReservationRepository;

/** In-memory ReservationRepository, used by the fast use-case tests. */
public class InMemoryReservationRepository implements ReservationRepository {
    private final AtomicInteger sequence = new AtomicInteger(1000);
    private final Map<String, Reservation> reservations = new ConcurrentHashMap<>();

    @Override
    public String nextId() {
        return "RES-" + sequence.getAndIncrement();
    }

    @Override
    public Optional<Reservation> findById(String reservationId) {
        return Optional.ofNullable(reservations.get(reservationId));
    }

    @Override
    public List<Reservation> findAll() {
        return List.copyOf(reservations.values());
    }

    @Override
    public List<Reservation> findByGuestId(String guestId) {
        return reservations.values().stream().filter(r -> r.getGuest().getId().equals(guestId)).toList();
    }

    @Override
    public List<Reservation> findByRoomNumber(String roomNumber) {
        return reservations.values().stream().filter(r -> r.getRoom().getRoomNumber().equals(roomNumber)).toList();
    }

    @Override
    public void save(Reservation reservation) {
        reservations.put(reservation.getId(), reservation);
    }
}
