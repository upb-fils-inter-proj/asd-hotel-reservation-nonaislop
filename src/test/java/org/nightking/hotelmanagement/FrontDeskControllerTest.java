package org.nightking.hotelmanagement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.nightking.hotelmanagement.controller.FrontDeskController;
import org.nightking.hotelmanagement.entity.Room;
import org.nightking.hotelmanagement.entity.RoomStatus;
import org.nightking.hotelmanagement.gateway.PaymentGateway;
import org.nightking.hotelmanagement.gateway.ReservationRepository;
import org.nightking.hotelmanagement.gateway.RoomRepository;
import org.nightking.hotelmanagement.gateway.ShopGateway;
import org.nightking.hotelmanagement.infrastructure.InMemoryReservationRepository;
import org.nightking.hotelmanagement.infrastructure.InMemoryRoomRepository;
import org.nightking.hotelmanagement.model.BookRoomRequest;
import org.nightking.hotelmanagement.model.GuestDetails;
import org.nightking.hotelmanagement.model.ReservationResponse;
import org.nightking.hotelmanagement.usecase.BookRoomInteractor;
import org.nightking.hotelmanagement.usecase.CheckInInteractor;
import org.nightking.hotelmanagement.usecase.CheckOutInteractor;
import org.nightking.hotelmanagement.usecase.ReservationEventPublisher;

/** Hotel management on its own: the shop and payment services are replaced by stub gateways. */
class FrontDeskControllerTest {
    private static final LocalDate IN = LocalDate.of(2027, 1, 10);
    private static final LocalDate OUT = LocalDate.of(2027, 1, 12);

    private final List<String> events = new ArrayList<>();
    private final Room room = new Room("101", "Standard", 80.0);

    private FrontDeskController frontDesk(boolean paymentApproved) {
        ReservationEventPublisher publisher = new ReservationEventPublisher();
        publisher.addObserver((reservation, event) -> events.add(event));

        RoomRepository rooms = new InMemoryRoomRepository();
        rooms.save(room);
        ReservationRepository reservations = new InMemoryReservationRepository();
        ShopGateway shop = (rate, nights, breakfast, spa, transfer) -> rate * nights;
        PaymentGateway payment = (reservationId, amount) -> paymentApproved;

        return new FrontDeskController(
                new BookRoomInteractor(rooms, reservations, shop, payment, publisher),
                new CheckInInteractor(rooms, reservations, publisher),
                new CheckOutInteractor(rooms, reservations, publisher));
    }

    private static BookRoomRequest booking(String roomNumber, LocalDate checkIn, LocalDate checkOut) {
        return new BookRoomRequest(new GuestDetails("G-1", "Test Guest", "guest@example.com", "+40000"),
                roomNumber, checkIn, checkOut, false, false, false);
    }

    @Test
    void approvedPaymentConfirmsBookingAtTheShopPrice() {
        ReservationResponse reservation = frontDesk(true).bookRoom(booking("101", IN, OUT)).orElseThrow();

        assertEquals("RES-1000", reservation.reservationId());
        assertEquals("CONFIRMED", reservation.state());
        assertEquals(160.0, reservation.totalPrice(), 1e-9);
        assertEquals("Test Guest", reservation.guestName());
        assertEquals(List.of("CONFIRMED"), events);
    }

    @Test
    void fullStayMovesThroughLifecycleAndRoomStatus() {
        FrontDeskController desk = frontDesk(true);
        String id = desk.bookRoom(booking("101", IN, OUT)).orElseThrow().reservationId();

        assertEquals("CHECKED_IN", desk.checkIn(id).orElseThrow().state());
        assertEquals(RoomStatus.OCCUPIED, room.getStatus());

        assertEquals("COMPLETED", desk.checkOut(id).orElseThrow().state());
        assertEquals(RoomStatus.AVAILABLE, room.getStatus());
        assertEquals(List.of("CONFIRMED", "CHECKED_IN", "CHECKED_OUT"), events);
    }

    @Test
    void declinedPaymentLeavesReservationPendingAndBlocksCheckIn() {
        FrontDeskController desk = frontDesk(false);
        ReservationResponse reservation = desk.bookRoom(booking("101", IN, OUT)).orElseThrow();
        assertEquals("PENDING", reservation.state());

        assertEquals("PENDING", desk.checkIn(reservation.reservationId()).orElseThrow().state());
        assertEquals(RoomStatus.AVAILABLE, room.getStatus());
        assertTrue(events.isEmpty());
    }

    @Test
    void checkOutBeforeCheckInIsRefused() {
        FrontDeskController desk = frontDesk(true);
        String id = desk.bookRoom(booking("101", IN, OUT)).orElseThrow().reservationId();

        assertEquals("CONFIRMED", desk.checkOut(id).orElseThrow().state());
        assertEquals(RoomStatus.AVAILABLE, room.getStatus());
    }

    @Test
    void rejectsUnknownRoom() {
        assertTrue(frontDesk(true).bookRoom(booking("999", IN, OUT)).isEmpty());
    }

    @Test
    void rejectsCheckOutBeforeCheckIn() {
        assertTrue(frontDesk(true).bookRoom(booking("101", OUT, IN)).isEmpty());
    }

    @Test
    void rejectsZeroNightStay() {
        assertTrue(frontDesk(true).bookRoom(booking("101", IN, IN)).isEmpty());
    }

    @Test
    void rejectsOccupiedRoom() {
        FrontDeskController desk = frontDesk(true);
        desk.checkIn(desk.bookRoom(booking("101", IN, OUT)).orElseThrow().reservationId());

        assertTrue(desk.bookRoom(booking("101", IN, OUT)).isEmpty());
    }

    @Test
    void unknownReservationIdIsRejected() {
        FrontDeskController desk = frontDesk(true);
        assertTrue(desk.checkIn("RES-404").isEmpty());
        assertTrue(desk.checkOut("RES-404").isEmpty());
    }
}
