package org.nightking.hotelmanagement.usecase;

/** Checks the guest out and frees the room. */
public interface CheckOutUseCase {
    void execute(String reservationId, ReservationPresenter presenter);
}
