package org.nightking.hotelmanagement.infrastructure.persistence;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.nightking.hotelmanagement.entity.Reservation;
import org.nightking.hotelmanagement.entity.lifecycle.ReservationState;
import org.nightking.hotelmanagement.gateway.ReservationRepository;
import org.nightking.hotelmanagement.gateway.RoomUnavailableException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * ReservationRepository gateway backed by the hotel database. A reservation whose state holds the
 * room owns one row per night in room_nights; that table's primary key (room, night) rejects any
 * second reservation for the same night, even when two bookings arrive at the same moment.
 */
public class JpaReservationRepository implements ReservationRepository {
    private final ReservationJpaRepository reservations;
    private final RoomJpaRepository rooms;
    private final AccountJpaRepository accounts;
    private final TransactionTemplate transaction;

    public JpaReservationRepository(ReservationJpaRepository reservations,
                                    RoomJpaRepository rooms,
                                    AccountJpaRepository accounts,
                                    PlatformTransactionManager transactionManager) {
        this.reservations = reservations;
        this.rooms = rooms;
        this.accounts = accounts;
        this.transaction = new TransactionTemplate(transactionManager);
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
        try {
            transaction.executeWithoutResult(status -> {
                reservations.saveAndFlush(toEntity(reservation));
                syncHeldNights(reservation);
            });
        } catch (DataIntegrityViolationException e) {
            throw new RoomUnavailableException(reservation.getRoom().getRoomNumber());
        }
    }

    /** Holds every night of the stay while the state holds the room, and releases them otherwise. */
    private void syncHeldNights(Reservation reservation) {
        String id = reservation.getId();
        if (!reservation.getState().holdsRoom()) {
            reservations.releaseNights(id);
            return;
        }
        if (reservations.countHeldNights(id) == 0) {
            for (var night = reservation.getCheckInDate(); night.isBefore(reservation.getCheckOutDate()); night = night.plusDays(1)) {
                reservations.holdNight(reservation.getRoom().getRoomNumber(), night, id);
            }
        }
    }

    private ReservationJpaEntity toEntity(Reservation reservation) {
        return new ReservationJpaEntity(
                reservation.getId(),
                accounts.getReferenceById(reservation.getGuest().getId()),
                rooms.getReferenceById(reservation.getRoom().getRoomNumber()),
                reservation.getCheckInDate(),
                reservation.getCheckOutDate(),
                BigDecimal.valueOf(reservation.getTotalPrice()),
                reservation.getState().getName(),
                reservation.getPaymentReference(),
                reservation.getPaidWith());
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
        reservation.recordPayment(entity.getPaymentReference(), entity.getPaidWith());
        return reservation;
    }
}
