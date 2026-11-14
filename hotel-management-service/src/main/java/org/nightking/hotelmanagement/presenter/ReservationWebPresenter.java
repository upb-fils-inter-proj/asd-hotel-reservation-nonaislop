package org.nightking.hotelmanagement.presenter;

import org.nightking.hotelmanagement.model.ReservationResponse;
import org.nightking.hotelmanagement.usecase.ReservationPresenter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** Turns the use case result into an HTTP response. One instance per request. */
public class ReservationWebPresenter implements ReservationPresenter {
    private ResponseEntity<Object> response;

    @Override
    public void presentBooked(ReservationResponse reservation) {
        response = ResponseEntity.status(HttpStatus.CREATED).body(ReservationViewModel.from(reservation));
    }

    @Override
    public void presentReservation(ReservationResponse reservation) {
        response = ResponseEntity.ok(ReservationViewModel.from(reservation));
    }

    @Override
    public void presentNotFound(String message) {
        response = error(HttpStatus.NOT_FOUND, message);
    }

    @Override
    public void presentRejected(String message) {
        response = error(HttpStatus.CONFLICT, message);
    }

    @Override
    public void presentInvalidRequest(String message) {
        response = error(HttpStatus.BAD_REQUEST, message);
    }

    public ResponseEntity<Object> response() {
        if (response == null) {
            throw new IllegalStateException("The use case did not present a result.");
        }
        return response;
    }

    private static ResponseEntity<Object> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ErrorViewModel(message));
    }
}
