package org.nightking.hotelmanagement.infrastructure;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.nightking.hotelmanagement.entity.Reservation;
import org.nightking.hotelmanagement.gateway.ReservationRepository;

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
    public void save(Reservation reservation) {
        reservations.put(reservation.getId(), reservation);
    }
}
