package org.nightking.hotelmanagement.usecase;

/** Lists all reservations (administration), or only one guest's when a guest id is given. */
public interface ListReservationsUseCase {
    void execute(String guestIdOrNull, ReservationListPresenter presenter);
}
