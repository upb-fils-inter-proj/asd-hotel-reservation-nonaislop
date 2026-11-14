package org.nightking.hotelmanagement.presenter;

import java.util.List;
import org.nightking.hotelmanagement.model.ReservationResponse;
import org.nightking.hotelmanagement.usecase.ReservationListPresenter;
import org.springframework.http.ResponseEntity;

public class ReservationListWebPresenter implements ReservationListPresenter {
    private ResponseEntity<Object> response;

    @Override
    public void presentReservations(List<ReservationResponse> reservations) {
        response = ResponseEntity.ok(reservations.stream().map(ReservationViewModel::from).toList());
    }

    public ResponseEntity<Object> response() {
        if (response == null) {
            throw new IllegalStateException("The use case did not present a result.");
        }
        return response;
    }
}
