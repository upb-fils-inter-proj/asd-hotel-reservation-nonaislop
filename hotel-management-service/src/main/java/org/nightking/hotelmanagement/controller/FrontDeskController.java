package org.nightking.hotelmanagement.controller;

import org.nightking.hotelmanagement.model.BookRoomRequest;
import org.nightking.hotelmanagement.presenter.ReservationWebPresenter;
import org.nightking.hotelmanagement.usecase.BookRoomUseCase;
import org.nightking.hotelmanagement.usecase.CheckInUseCase;
import org.nightking.hotelmanagement.usecase.CheckOutUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Entry point of the hotel management service: booking, check-in and check-out. */
@RestController
@RequestMapping("/api/reservations")
public class FrontDeskController {
    private final BookRoomUseCase bookRoom;
    private final CheckInUseCase checkIn;
    private final CheckOutUseCase checkOut;

    public FrontDeskController(BookRoomUseCase bookRoom,
                               CheckInUseCase checkIn,
                               CheckOutUseCase checkOut) {
        this.bookRoom = bookRoom;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
    }

    @PostMapping
    public ResponseEntity<Object> bookRoom(@RequestBody BookRoomRequest request) {
        ReservationWebPresenter presenter = new ReservationWebPresenter();
        bookRoom.execute(request, presenter);
        return presenter.response();
    }

    @PostMapping("/{reservationId}/check-in")
    public ResponseEntity<Object> checkIn(@PathVariable String reservationId) {
        ReservationWebPresenter presenter = new ReservationWebPresenter();
        checkIn.execute(reservationId, presenter);
        return presenter.response();
    }

    @PostMapping("/{reservationId}/check-out")
    public ResponseEntity<Object> checkOut(@PathVariable String reservationId) {
        ReservationWebPresenter presenter = new ReservationWebPresenter();
        checkOut.execute(reservationId, presenter);
        return presenter.response();
    }
}
