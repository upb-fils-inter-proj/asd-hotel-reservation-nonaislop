package org.nightking.hotelmanagement.usecase;

/**
 * Cancels a reservation that has not started yet and refunds its payment.
 * A guest can only cancel their own reservations (guestIdOrNull set); an administrator can cancel any.
 */
public interface CancelReservationUseCase {
    void execute(String reservationId, String guestIdOrNull, ReservationPresenter presenter);
}
