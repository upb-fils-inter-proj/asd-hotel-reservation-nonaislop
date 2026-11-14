package org.nightking.hotelmanagement.presenter;

import java.time.LocalDate;
import java.util.List;
import org.nightking.hotelmanagement.model.RoomResponse;
import org.nightking.hotelmanagement.usecase.AvailabilityPresenter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class AvailabilityWebPresenter implements AvailabilityPresenter {
    private ResponseEntity<Object> response;

    /** JSON shape of an availability search. */
    public record AvailabilityViewModel(LocalDate checkIn, LocalDate checkOut, int nights, List<RoomViewModel> rooms) {}

    @Override
    public void presentAvailableRooms(LocalDate checkIn, LocalDate checkOut, int nights, List<RoomResponse> rooms) {
        response = ResponseEntity.ok(new AvailabilityViewModel(checkIn, checkOut, nights,
                rooms.stream().map(RoomViewModel::from).toList()));
    }

    @Override
    public void presentInvalidRequest(String message) {
        response = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorViewModel(message));
    }

    public ResponseEntity<Object> response() {
        if (response == null) {
            throw new IllegalStateException("The use case did not present a result.");
        }
        return response;
    }
}
