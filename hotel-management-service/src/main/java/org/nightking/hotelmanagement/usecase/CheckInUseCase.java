package org.nightking.hotelmanagement.usecase;

/** Checks the guest in and marks the room occupied. */
public interface CheckInUseCase {
    void execute(String reservationId, ReservationPresenter presenter);
}
