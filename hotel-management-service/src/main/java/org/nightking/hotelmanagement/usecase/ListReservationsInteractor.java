package org.nightking.hotelmanagement.usecase;

import java.util.List;
import org.nightking.hotelmanagement.entity.Reservation;
import org.nightking.hotelmanagement.gateway.ReservationRepository;
import org.nightking.hotelmanagement.model.ReservationResponse;

public class ListReservationsInteractor implements ListReservationsUseCase {
    private final ReservationRepository reservations;

    public ListReservationsInteractor(ReservationRepository reservations) {
        this.reservations = reservations;
    }

    @Override
    public void execute(String guestIdOrNull, ReservationListPresenter presenter) {
        List<Reservation> found = guestIdOrNull == null
                ? reservations.findAll()
                : reservations.findByGuestId(guestIdOrNull);
        presenter.presentReservations(found.stream().map(ReservationResponse::from).toList());
    }
}
