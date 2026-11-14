package org.nightking.hotelmanagement.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.nightking.hotelmanagement.entity.Room;
import org.nightking.hotelmanagement.entity.RoomStatus;
import org.nightking.hotelmanagement.gateway.ReservationRepository;
import org.nightking.hotelmanagement.gateway.RoomRepository;
import org.nightking.hotelmanagement.infrastructure.InMemoryReservationRepository;
import org.nightking.hotelmanagement.infrastructure.InMemoryRoomRepository;
import org.nightking.hotelmanagement.model.BookRoomRequest;
import org.nightking.hotelmanagement.model.GuestDetails;
import org.nightking.hotelmanagement.model.ReservationResponse;

/** The use cases on their own: shop and payment are stub gateways, results go to a recording presenter. */
class ReservationUseCasesTest {
    private static final LocalDate IN = LocalDate.of(2027, 1, 10);
    private static final LocalDate OUT = LocalDate.of(2027, 1, 12);

    static class RecordingPresenter implements ReservationPresenter {
        String outcome;
        ReservationResponse reservation;
        String message;

        @Override public void presentBooked(ReservationResponse r) { outcome = "BOOKED"; reservation = r; }
        @Override public void presentReservation(ReservationResponse r) { outcome = "UPDATED"; reservation = r; }
        @Override public void presentNotFound(String m) { outcome = "NOT_FOUND"; message = m; }
        @Override public void presentRejected(String m) { outcome = "REJECTED"; message = m; }
        @Override public void presentInvalidRequest(String m) { outcome = "INVALID"; message = m; }
    }

    private final List<String> events = new ArrayList<>();
    private final Room room = new Room("101", "Standard", 80.0);
    private BookRoomUseCase bookRoom;
    private CheckInUseCase checkIn;
    private CheckOutUseCase checkOut;

    private void hotel(boolean paymentApproved) {
        ReservationEventPublisher publisher = new ReservationEventPublisher();
        publisher.addObserver((reservation, event) -> events.add(event));
        RoomRepository rooms = new InMemoryRoomRepository();
        rooms.save(room);
        ReservationRepository reservations = new InMemoryReservationRepository();

        bookRoom = new BookRoomInteractor(rooms, reservations,
                (rate, nights, breakfast, spa, transfer) -> rate * nights,
                (reservationId, amount) -> paymentApproved,
                publisher);
        checkIn = new CheckInInteractor(rooms, reservations, publisher);
        checkOut = new CheckOutInteractor(rooms, reservations, publisher);
    }

    private RecordingPresenter book(String roomNumber, LocalDate checkInDate, LocalDate checkOutDate) {
        RecordingPresenter presenter = new RecordingPresenter();
        bookRoom.execute(new BookRoomRequest(new GuestDetails("G-1", "Test Guest", "guest@example.com", "+40000"),
                roomNumber, checkInDate, checkOutDate, false, false, false), presenter);
        return presenter;
    }

    private RecordingPresenter checkIn(String id) {
        RecordingPresenter presenter = new RecordingPresenter();
        checkIn.execute(id, presenter);
        return presenter;
    }

    private RecordingPresenter checkOut(String id) {
        RecordingPresenter presenter = new RecordingPresenter();
        checkOut.execute(id, presenter);
        return presenter;
    }

    @Test
    void approvedPaymentConfirmsBookingAtTheShopPrice() {
        hotel(true);
        RecordingPresenter result = book("101", IN, OUT);

        assertEquals("BOOKED", result.outcome);
        assertEquals("RES-1000", result.reservation.reservationId());
        assertEquals("CONFIRMED", result.reservation.state());
        assertEquals(160.0, result.reservation.totalPrice(), 1e-9);
        assertEquals(List.of("CONFIRMED"), events);
    }

    @Test
    void fullStayMovesThroughLifecycleAndRoomStatus() {
        hotel(true);
        String id = book("101", IN, OUT).reservation.reservationId();

        assertEquals("CHECKED_IN", checkIn(id).reservation.state());
        assertEquals(RoomStatus.OCCUPIED, room.getStatus());
        assertEquals("COMPLETED", checkOut(id).reservation.state());
        assertEquals(RoomStatus.AVAILABLE, room.getStatus());
        assertEquals(List.of("CONFIRMED", "CHECKED_IN", "CHECKED_OUT"), events);
    }

    @Test
    void declinedPaymentLeavesReservationPendingAndBlocksCheckIn() {
        hotel(false);
        RecordingPresenter booked = book("101", IN, OUT);
        assertEquals("BOOKED", booked.outcome);
        assertEquals("PENDING", booked.reservation.state());

        assertEquals("REJECTED", checkIn(booked.reservation.reservationId()).outcome);
        assertEquals(RoomStatus.AVAILABLE, room.getStatus());
        assertTrue(events.isEmpty());
    }

    @Test
    void checkOutBeforeCheckInIsRejected() {
        hotel(true);
        String id = book("101", IN, OUT).reservation.reservationId();
        assertEquals("REJECTED", checkOut(id).outcome);
        assertEquals(RoomStatus.AVAILABLE, room.getStatus());
    }

    @Test
    void unknownRoomIsNotFound() {
        hotel(true);
        assertEquals("NOT_FOUND", book("999", IN, OUT).outcome);
    }

    @Test
    void reversedOrZeroNightDatesAreInvalid() {
        hotel(true);
        assertEquals("INVALID", book("101", OUT, IN).outcome);
        assertEquals("INVALID", book("101", IN, IN).outcome);
    }

    @Test
    void missingFieldsAreInvalid() {
        hotel(true);
        assertEquals("INVALID", book("101", null, OUT).outcome);
    }

    @Test
    void occupiedRoomIsRejected() {
        hotel(true);
        checkIn(book("101", IN, OUT).reservation.reservationId());
        assertEquals("REJECTED", book("101", IN, OUT).outcome);
    }

    @Test
    void unknownReservationIsNotFound() {
        hotel(true);
        assertEquals("NOT_FOUND", checkIn("RES-404").outcome);
        assertEquals("NOT_FOUND", checkOut("RES-404").outcome);
    }
}
