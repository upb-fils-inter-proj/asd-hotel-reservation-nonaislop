package org.nightking.hotelmanagement.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.nightking.hotelmanagement.entity.Account;
import org.nightking.hotelmanagement.entity.AccountRole;
import org.nightking.hotelmanagement.entity.Room;
import org.nightking.hotelmanagement.entity.RoomStatus;
import org.nightking.hotelmanagement.gateway.AccountRepository;
import org.nightking.hotelmanagement.gateway.ReservationRepository;
import org.nightking.hotelmanagement.gateway.RoomRepository;
import org.nightking.hotelmanagement.infrastructure.InMemoryAccountRepository;
import org.nightking.hotelmanagement.infrastructure.InMemoryReservationRepository;
import org.nightking.hotelmanagement.infrastructure.InMemoryRoomRepository;
import org.nightking.hotelmanagement.model.BookRoomRequest;
import org.nightking.hotelmanagement.model.ReservationResponse;
import org.nightking.hotelmanagement.model.RoomResponse;

/** The reservation use cases on their own: in-memory gateways, stub shop and payment, recording presenters. */
class ReservationUseCasesTest {
    private static final LocalDate IN = LocalDate.of(2027, 1, 10);
    private static final LocalDate OUT = LocalDate.of(2027, 1, 12);
    private static final String GUEST = "guest-1";

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
    private final RoomRepository rooms = new InMemoryRoomRepository();
    private final ReservationRepository reservations = new InMemoryReservationRepository();
    private final AccountRepository accounts = new InMemoryAccountRepository();
    private BookRoomUseCase bookRoom;
    private CheckInUseCase checkIn;
    private CheckOutUseCase checkOut;

    private void hotel(boolean paymentApproved) {
        ReservationEventPublisher publisher = new ReservationEventPublisher();
        publisher.addObserver((reservation, event) -> events.add(event));
        rooms.save(room);
        accounts.save(new Account(GUEST, "alex", "hash", AccountRole.GUEST, "Alex Morgan", "alex@example.com", "+40"));

        bookRoom = new BookRoomInteractor(accounts, rooms, reservations,
                (rate, nights, breakfast, spa, transfer) -> rate * nights,
                (reservationId, amount) -> paymentApproved,
                publisher);
        checkIn = new CheckInInteractor(rooms, reservations, publisher);
        checkOut = new CheckOutInteractor(rooms, reservations, publisher);
    }

    private RecordingPresenter book(String guestId, String roomNumber, LocalDate from, LocalDate to) {
        RecordingPresenter presenter = new RecordingPresenter();
        bookRoom.execute(new BookRoomRequest(guestId, roomNumber, from, to, false, false, false), presenter);
        return presenter;
    }

    private RecordingPresenter book(LocalDate from, LocalDate to) {
        return book(GUEST, "101", from, to);
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
    void approvedPaymentConfirmsBookingForTheGuestAccount() {
        hotel(true);
        RecordingPresenter result = book(IN, OUT);

        assertEquals("BOOKED", result.outcome);
        assertEquals("RES-1000", result.reservation.reservationId());
        assertEquals(GUEST, result.reservation.guestId());
        assertEquals("Alex Morgan", result.reservation.guestName());
        assertEquals("CONFIRMED", result.reservation.state());
        assertEquals(160.0, result.reservation.totalPrice(), 1e-9);
        assertEquals(List.of("CONFIRMED"), events);
    }

    @Test
    void unknownGuestAccountCannotBook() {
        hotel(true);
        assertEquals("NOT_FOUND", book("nobody", "101", IN, OUT).outcome);
    }

    @Test
    void fullStayMovesThroughLifecycleAndRoomStatus() {
        hotel(true);
        String id = book(IN, OUT).reservation.reservationId();

        assertEquals("CHECKED_IN", checkIn(id).reservation.state());
        assertEquals(RoomStatus.OCCUPIED, room.getStatus());
        assertEquals("COMPLETED", checkOut(id).reservation.state());
        assertEquals(RoomStatus.AVAILABLE, room.getStatus());
        assertEquals(List.of("CONFIRMED", "CHECKED_IN", "CHECKED_OUT"), events);
    }

    @Test
    void overlappingDatesAreRejectedEvenBeforeCheckIn() {
        hotel(true);
        book(IN, OUT);
        assertEquals("REJECTED", book(IN.plusDays(1), OUT.plusDays(3)).outcome);
    }

    @Test
    void backToBackStaysAreAllowed() {
        hotel(true);
        book(IN, OUT);
        assertEquals("BOOKED", book(OUT, OUT.plusDays(2)).outcome);
    }

    @Test
    void completedStayFreesTheDates() {
        hotel(true);
        String id = book(IN, OUT).reservation.reservationId();
        checkIn(id);
        checkOut(id);
        assertEquals("BOOKED", book(IN, OUT).outcome);
    }

    @Test
    void declinedPaymentLeavesReservationPendingDoesNotHoldTheRoomAndBlocksCheckIn() {
        hotel(false);
        RecordingPresenter booked = book(IN, OUT);
        assertEquals("BOOKED", booked.outcome);
        assertEquals("PENDING", booked.reservation.state());

        assertEquals("REJECTED", checkIn(booked.reservation.reservationId()).outcome);
        assertEquals(RoomStatus.AVAILABLE, room.getStatus());
        assertTrue(events.isEmpty());
        assertEquals("BOOKED", book(IN, OUT).outcome, "an unpaid reservation does not block the dates");
    }

    @Test
    void checkOutBeforeCheckInIsRejected() {
        hotel(true);
        assertEquals("REJECTED", checkOut(book(IN, OUT).reservation.reservationId()).outcome);
    }

    @Test
    void unknownRoomIsNotFound() {
        hotel(true);
        assertEquals("NOT_FOUND", book(GUEST, "999", IN, OUT).outcome);
    }

    @Test
    void reversedZeroNightOrMissingDatesAreInvalid() {
        hotel(true);
        assertEquals("INVALID", book(OUT, IN).outcome);
        assertEquals("INVALID", book(IN, IN).outcome);
        assertEquals("INVALID", book(null, OUT).outcome);
    }

    @Test
    void unknownReservationIsNotFound() {
        hotel(true);
        assertEquals("NOT_FOUND", checkIn("RES-404").outcome);
        assertEquals("NOT_FOUND", checkOut("RES-404").outcome);
    }

    @Test
    void listsReservationsForAllOrForOneGuest() {
        hotel(true);
        accounts.save(new Account("guest-2", "sam", "hash", AccountRole.GUEST, "Sam Lee", "sam@example.com", "+41"));
        book(IN, OUT);
        book("guest-2", "101", OUT, OUT.plusDays(1));

        List<List<ReservationResponse>> seen = new ArrayList<>();
        ListReservationsUseCase list = new ListReservationsInteractor(reservations);
        list.execute(null, seen::add);
        list.execute("guest-2", seen::add);

        assertEquals(2, seen.get(0).size());
        assertEquals(1, seen.get(1).size());
        assertEquals("Sam Lee", seen.get(1).get(0).guestName());
    }

    @Test
    void listsRooms() {
        hotel(true);
        List<List<RoomResponse>> seen = new ArrayList<>();
        new ListRoomsInteractor(rooms).execute(seen::add);
        assertEquals(List.of(new RoomResponse("101", "Standard", 80.0, "AVAILABLE")), seen.get(0));
    }
}
