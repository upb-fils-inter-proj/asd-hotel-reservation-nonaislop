package org.nightking.hotelmanagement.controller;

import java.util.Optional;
import org.nightking.hotelmanagement.model.BookRoomRequest;
import org.nightking.hotelmanagement.model.ReservationResponse;
import org.nightking.hotelmanagement.usecase.BookRoomUseCase;
import org.nightking.hotelmanagement.usecase.CheckInUseCase;
import org.nightking.hotelmanagement.usecase.CheckOutUseCase;

/** Entry point of the hotel management service: booking, check-in and check-out. */
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

    public Optional<ReservationResponse> bookRoom(BookRoomRequest request) {
        System.out.println("\n=== BOOKING THROUGH THE FRONT DESK ===");
        return bookRoom.execute(request);
    }

    public Optional<ReservationResponse> checkIn(String reservationId) {
        System.out.println("\n=== CHECK-IN THROUGH THE FRONT DESK ===");
        return checkIn.execute(reservationId);
    }

    public Optional<ReservationResponse> checkOut(String reservationId) {
        System.out.println("\n=== CHECK-OUT THROUGH THE FRONT DESK ===");
        return checkOut.execute(reservationId);
    }
}
