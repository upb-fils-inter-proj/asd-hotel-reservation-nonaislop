package org.nightking.hotelmanagement.usecase;

import java.util.List;
import org.nightking.hotelmanagement.model.ReservationResponse;

public interface ReservationListPresenter {
    void presentReservations(List<ReservationResponse> reservations);
}
