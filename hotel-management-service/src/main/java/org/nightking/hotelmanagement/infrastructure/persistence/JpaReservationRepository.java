package org.nightking.hotelmanagement.infrastructure.persistence;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.nightking.hotelmanagement.entity.Reservation;
import org.nightking.hotelmanagement.entity.lifecycle.ReservationState;
import org.nightking.hotelmanagement.gateway.ReservationRepository;

/** ReservationRepository gateway backed by the hotel database. */
public class JpaReservationRepository implements ReservationRepository {
    private final ReservationJpaRepository reservations;
    private final RoomJpaRepository rooms;
    private final AccountJpaRepository accounts;

    public JpaReservationRepository(ReservationJpaRepository reservations,
                                    RoomJpaRepository rooms,
                                    AccountJpaRepository accounts) {
        this.reservations = reservations;
        this.rooms = rooms;
        this.accounts = accounts;
    }

    @Override
    public String nextId() {
        return "RES-" + reservations.nextReservationNumber();
    }

    @Override
    public Optional<Reservation> findById(String reservationId) {
        return reservations.findById(reservationId).map(JpaReservationRepository::toDomain);
    }

    @Override
    public List<Reservation> findAll() {
        return reservations.findAllByOrderByCheckInDescIdDesc().stream().map(JpaReservationRepository::toDomain).toList();
    }

    @Override
    public List<Reservation> findByGuestId(String guestId) {
        return reservations.findByGuest_IdOrderByCheckInDescIdDesc(guestId).stream()
                .map(JpaReservationRepository::toDomain).toList();
    }

    @Override
    public List<Reservation> findByRoomNumber(String roomNumber) {
        return reservations.findByRoom_RoomNumber(roomNumber).stream().map(JpaReservationRepository::toDomain).toList();
    }

    @Override
    public void save(Reservation reservation) {
        reservations.save(new ReservationJpaEntity(
                reservation.getId(),
                accounts.getReferenceById(reservation.getGuest().getId()),
                rooms.getReferenceById(reservation.getRoom().getRoomNumber()),
                reservation.getCheckInDate(),
                reservation.getCheckOutDate(),
                BigDecimal.valueOf(reservation.getTotalPrice()),
                reservation.getState().getName()));
    }

    private static Reservation toDomain(ReservationJpaEntity entity) {
        Reservation reservation = new Reservation(
                entity.getId(),
                JpaAccountRepository.toDomain(entity.getGuest()).asGuest(),
                JpaRoomRepository.toDomain(entity.getRoom()),
                entity.getCheckIn(),
                entity.getCheckOut(),
                entity.getTotalPrice().doubleValue());
        reservation.setState(ReservationState.fromName(entity.getState()));
        return reservation;
    }
}
