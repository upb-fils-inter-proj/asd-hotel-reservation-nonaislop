package org.nightking.hotelmanagement.usecase;

import java.util.Optional;
import org.nightking.hotelmanagement.entity.Reservation;
import org.nightking.hotelmanagement.entity.Room;
import org.nightking.hotelmanagement.entity.RoomStatus;
import org.nightking.hotelmanagement.gateway.ReservationRepository;
import org.nightking.hotelmanagement.gateway.RoomRepository;
import org.nightking.hotelmanagement.model.ReservationResponse;

public class CheckInInteractor implements CheckInUseCase {
    private final RoomRepository rooms;
    private final ReservationRepository reservations;
    private final ReservationEventPublisher eventPublisher;

    public CheckInInteractor(RoomRepository rooms,
                             ReservationRepository reservations,
                             ReservationEventPublisher eventPublisher) {
        this.rooms = rooms;
        this.reservations = reservations;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void execute(String reservationId, ReservationPresenter presenter) {
        Optional<Reservation> found = reservations.findById(reservationId);
        if (found.isEmpty()) {
            presenter.presentNotFound("Reservation " + reservationId + " not found.");
            return;
        }
        Reservation reservation = found.get();

        String previousState = reservation.getState().getName();
        reservation.checkIn();
        if (previousState.equals(reservation.getState().getName())) {
            presenter.presentRejected("Cannot check in reservation " + reservationId
                    + " while it is " + previousState + ".");
            return;
        }

        Room room = reservation.getRoom();
        room.updateStatus(RoomStatus.OCCUPIED);
        rooms.save(room);
        reservations.save(reservation);
        eventPublisher.notifyObservers(reservation, "CHECKED_IN");
        presenter.presentReservation(ReservationResponse.from(reservation));
    }
}
