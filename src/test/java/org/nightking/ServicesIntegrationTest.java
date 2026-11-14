package org.nightking;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.nightking.hotelmanagement.controller.FrontDeskController;
import org.nightking.hotelmanagement.model.BookRoomRequest;
import org.nightking.hotelmanagement.model.GuestDetails;
import org.nightking.hotelmanagement.model.ReservationResponse;

/** All three services wired exactly as Main wires them. */
class ServicesIntegrationTest {

    @Test
    void bookingCheckInAndCheckOutAcrossAllThreeServices() {
        FrontDeskController frontDesk = Main.createHotelManagementService(
                Main.createShopService(), Main.createPaymentService());

        ReservationResponse reservation = frontDesk.bookRoom(new BookRoomRequest(
                new GuestDetails("G-01", "Alex Morgan", "alex@example.com", "+40123456789"),
                "204", LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 13),
                true, true, false)).orElseThrow();

        // shop: 100 * 3 nights * 1.2 seasonal = 360, + breakfast 45, + spa 45
        assertEquals(450.0, reservation.totalPrice(), 1e-9);
        // payment approved by PayFlow -> confirmed
        assertEquals("CONFIRMED", reservation.state());

        assertEquals("CHECKED_IN", frontDesk.checkIn(reservation.reservationId()).orElseThrow().state());
        assertEquals("COMPLETED", frontDesk.checkOut(reservation.reservationId()).orElseThrow().state());
    }
}
