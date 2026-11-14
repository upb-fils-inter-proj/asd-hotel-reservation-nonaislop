package org.nightking.hotelmanagement.controller;

import org.nightking.hotelmanagement.model.BookRoomRequest;
import org.nightking.hotelmanagement.presenter.ReservationListWebPresenter;
import org.nightking.hotelmanagement.presenter.ReservationWebPresenter;
import org.nightking.hotelmanagement.usecase.BookRoomUseCase;
import org.nightking.hotelmanagement.usecase.CheckInUseCase;
import org.nightking.hotelmanagement.usecase.CheckOutUseCase;
import org.nightking.hotelmanagement.usecase.ListReservationsUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Entry point of the hotel management service: booking, check-in, check-out and listing. */
@RestController
@RequestMapping("/api/reservations")
public class FrontDeskController {
    private final BookRoomUseCase bookRoom;
    private final CheckInUseCase checkIn;
    private final CheckOutUseCase checkOut;
    private final ListReservationsUseCase listReservations;

    public FrontDeskController(BookRoomUseCase bookRoom,
                               CheckInUseCase checkIn,
                               CheckOutUseCase checkOut,
                               ListReservationsUseCase listReservations) {
        this.bookRoom = bookRoom;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.listReservations = listReservations;
    }

    @GetMapping
    public ResponseEntity<Object> list(@RequestParam(required = false) String guestId) {
        ReservationListWebPresenter presenter = new ReservationListWebPresenter();
        listReservations.execute(guestId, presenter);
        return presenter.response();
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
