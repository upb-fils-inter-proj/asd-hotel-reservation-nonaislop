package org.nightking.hotelmanagement.usecase;

import org.nightking.hotelmanagement.model.ReservationResponse;

/** Output port shared by the reservation use cases: every possible outcome has its own method. */
public interface ReservationPresenter {
    /** A reservation was created (confirmed, or pending if the payment was declined). */
    void presentBooked(ReservationResponse reservation);

    /** An existing reservation changed state. */
    void presentReservation(ReservationResponse reservation);

    void presentNotFound(String message);

    /** The request was valid but a business rule refused it (room taken, wrong lifecycle state). */
    void presentRejected(String message);

    void presentInvalidRequest(String message);
}
