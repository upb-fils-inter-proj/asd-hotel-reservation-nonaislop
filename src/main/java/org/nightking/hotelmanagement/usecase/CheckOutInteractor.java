package org.nightking.hotelmanagement.usecase;

import java.util.Optional;
import org.nightking.hotelmanagement.entity.Reservation;
import org.nightking.hotelmanagement.entity.Room;
import org.nightking.hotelmanagement.entity.RoomStatus;
import org.nightking.hotelmanagement.gateway.ReservationRepository;
import org.nightking.hotelmanagement.gateway.RoomRepository;
import org.nightking.hotelmanagement.model.ReservationResponse;

public class CheckOutInteractor implements CheckOutUseCase {
    private final RoomRepository rooms;
    private final ReservationRepository reservations;
    private final ReservationEventPublisher eventPublisher;

    public CheckOutInteractor(RoomRepository rooms,
                              ReservationRepository reservations,
                              ReservationEventPublisher eventPublisher) {
        this.rooms = rooms;
        this.reservations = reservations;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Optional<ReservationResponse> execute(String reservationId) {
        Optional<Reservation> found = reservations.findById(reservationId);
        if (found.isEmpty()) {
            System.out.println("Check-out failed: reservation " + reservationId + " not found.");
            return Optional.empty();
        }
        Reservation reservation = found.get();

        String previousState = reservation.getState().getName();
        reservation.checkOut();
        if (!previousState.equals(reservation.getState().getName())) {
            Room room = reservation.getRoom();
            room.updateStatus(RoomStatus.AVAILABLE);
            rooms.save(room);
            reservations.save(reservation);
            eventPublisher.notifyObservers(reservation, "CHECKED_OUT");
        }
        return Optional.of(ReservationResponse.from(reservation));
    }
}
