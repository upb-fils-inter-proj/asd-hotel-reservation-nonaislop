package org.nightking.hotelmanagement.usecase;

import java.util.Optional;
import org.nightking.hotelmanagement.entity.Reservation;
import org.nightking.hotelmanagement.gateway.PaymentGateway;
import org.nightking.hotelmanagement.gateway.ReservationRepository;
import org.nightking.hotelmanagement.model.ReservationResponse;

public class CancelReservationInteractor implements CancelReservationUseCase {
    private final ReservationRepository reservations;
    private final PaymentGateway payment;
    private final ReservationEventPublisher eventPublisher;

    public CancelReservationInteractor(ReservationRepository reservations, PaymentGateway payment,
                                       ReservationEventPublisher eventPublisher) {
        this.reservations = reservations;
        this.payment = payment;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void execute(String reservationId, String guestIdOrNull, ReservationPresenter presenter) {
        Optional<Reservation> found = reservations.findById(reservationId);
        // A guest asking about someone else's reservation gets the same answer as for a missing one.
        if (found.isEmpty() || (guestIdOrNull != null && !found.get().getGuest().getId().equals(guestIdOrNull))) {
            presenter.presentNotFound("Reservation " + reservationId + " not found.");
            return;
        }
        Reservation reservation = found.get();

        String previousState = reservation.getState().getName();
        reservation.cancel();
        if (previousState.equals(reservation.getState().getName())) {
            presenter.presentRejected("Cannot cancel reservation " + reservationId + " while it is " + previousState + ".");
            return;
        }

        // Refund before saving: if the refund fails, the reservation stays as it was.
        if (reservation.getPaymentReference() != null) {
            payment.refund(reservation.getPaymentReference());
        }
        reservations.save(reservation);
        eventPublisher.notifyObservers(reservation, "CANCELLED");
        presenter.presentReservation(ReservationResponse.from(reservation));
    }
}
