package org.nightking.hotelmanagement.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nightking.hotelmanagement.entity.Account;
import org.nightking.hotelmanagement.entity.AccountRole;
import org.nightking.hotelmanagement.entity.Reservation;
import org.nightking.hotelmanagement.entity.Room;
import org.nightking.hotelmanagement.entity.RoomStatus;
import org.nightking.hotelmanagement.gateway.PaymentGateway;
import org.nightking.hotelmanagement.gateway.RoomUnavailableException;
import org.nightking.hotelmanagement.gateway.ShopGateway;
import org.nightking.hotelmanagement.infrastructure.InMemoryAccountRepository;
import org.nightking.hotelmanagement.infrastructure.InMemoryReservationRepository;
import org.nightking.hotelmanagement.infrastructure.InMemoryRoomRepository;
import org.nightking.hotelmanagement.model.BookRoomRequest;
import org.nightking.hotelmanagement.model.BookingQuoteResponse;
import org.nightking.hotelmanagement.model.CardDetails;
import org.nightking.hotelmanagement.model.QuoteRequest;
import org.nightking.hotelmanagement.model.ReservationResponse;
import org.nightking.hotelmanagement.model.RoomResponse;

/** The reservation use cases on their own: in-memory gateways, a fake shop and payment, recording presenters. */
class ReservationUseCasesTest {
    private static final Clock TODAY = Clock.fixed(Instant.parse("2026-11-01T09:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate IN = LocalDate.of(2026, 12, 10);
    private static final LocalDate OUT = LocalDate.of(2026, 12, 12);
    private static final String GUEST = "guest-1";
    private static final CardDetails CARD = new CardDetails("Alex Morgan", "4242424242424242", 12, 2030, "123");

    static class RecordingPresenter implements ReservationPresenter {
        String outcome;
        ReservationResponse reservation;
        String message;

        @Override public void presentBooked(ReservationResponse r) { outcome = "BOOKED"; reservation = r; }
        @Override public void presentReservation(ReservationResponse r) { outcome = "UPDATED"; reservation = r; }
        @Override public void presentNotFound(String m) { outcome = "NOT_FOUND"; message = m; }
        @Override public void presentRejected(String m) { outcome = "REJECTED"; message = m; }
        @Override public void presentInvalidRequest(String m) { outcome = "INVALID"; message = m; }
        @Override public void presentPaymentDeclined(String m) { outcome = "DECLINED"; message = m; }
    }

    /** Approves or declines every charge, and remembers refunds. */
    static class FakePayment implements PaymentGateway {
        boolean approve = true;
        final List<String> refunds = new ArrayList<>();
        int charges;

        @Override
        public ChargeResult charge(String reservationId, double amount, Card card) {
            charges++;
            return approve
                    ? new ChargeResult(true, "PAY-" + charges, "Visa •••• 4242", null)
                    : new ChargeResult(false, "PAY-" + charges, "Visa •••• 0002", "Insufficient funds.");
        }

        @Override
        public void refund(String paymentReference) {
            refunds.add(paymentReference);
        }
    }

    private final List<String> events = new ArrayList<>();
    private final Room room = new Room("101", "Standard", 80.0);
    private final InMemoryRoomRepository rooms = new InMemoryRoomRepository();
    private final InMemoryReservationRepository reservations = new InMemoryReservationRepository();
    private final InMemoryAccountRepository accounts = new InMemoryAccountRepository();
    private final ShopGateway shop = (rate, nights, breakfast, spa, transfer) ->
            new ShopGateway.StayQuote(rate * nights + (breakfast ? 15.0 * nights : 0), breakfast ? "Room stay + Breakfast" : "Room stay");
    private final FakePayment payment = new FakePayment();
    private BookRoomUseCase bookRoom;
    private CheckInUseCase checkIn;
    private CheckOutUseCase checkOut;
    private CancelReservationUseCase cancel;

    @BeforeEach
    void hotel() {
        ReservationEventPublisher publisher = new ReservationEventPublisher();
        publisher.addObserver((reservation, event) -> events.add(event));
        rooms.save(room);
        accounts.save(new Account(GUEST, "alex", "hash", AccountRole.GUEST, "Alex Morgan", "alex@example.com", "+40"));

        bookRoom = new BookRoomInteractor(accounts, rooms, reservations, shop, payment, publisher, TODAY);
        checkIn = new CheckInInteractor(rooms, reservations, publisher);
        checkOut = new CheckOutInteractor(rooms, reservations, publisher);
        cancel = new CancelReservationInteractor(reservations, payment, publisher);
    }

    private RecordingPresenter book(String guestId, String roomNumber, LocalDate from, LocalDate to) {
        RecordingPresenter presenter = new RecordingPresenter();
        bookRoom.execute(new BookRoomRequest(guestId, roomNumber, from, to, false, false, false, CARD), presenter);
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

    private RecordingPresenter cancel(String id, String guestId) {
        RecordingPresenter presenter = new RecordingPresenter();
        cancel.execute(id, guestId, presenter);
        return presenter;
    }

    // --- booking and payment

    @Test
    void approvedCardConfirmsBookingAndRecordsThePayment() {
        RecordingPresenter result = book(IN, OUT);

        assertEquals("BOOKED", result.outcome);
        assertEquals("CONFIRMED", result.reservation.state());
        assertEquals(160.0, result.reservation.totalPrice(), 1e-9);
        assertEquals("PAY-1", result.reservation.paymentReference());
        assertEquals("Visa •••• 4242", result.reservation.paidWith());
        assertEquals(List.of("CONFIRMED"), events);
    }

    @Test
    void declinedCardBooksNothingAndFreesTheRoom() {
        payment.approve = false;
        RecordingPresenter declined = book(IN, OUT);
        assertEquals("DECLINED", declined.outcome);
        assertEquals("Insufficient funds.", declined.message);
        assertTrue(events.isEmpty());
        assertEquals("CANCELLED", reservations.findAll().get(0).getState().getName());

        payment.approve = true;
        assertEquals("BOOKED", book(IN, OUT).outcome, "the dates are free again for the next try");
    }

    @Test
    void cardIsRequired() {
        RecordingPresenter presenter = new RecordingPresenter();
        bookRoom.execute(new BookRoomRequest(GUEST, "101", IN, OUT, false, false, false, null), presenter);
        assertEquals("INVALID", presenter.outcome);
    }

    @Test
    void unknownGuestAccountOrRoomIsNotFound() {
        assertEquals("NOT_FOUND", book("nobody", "101", IN, OUT).outcome);
        assertEquals("NOT_FOUND", book(GUEST, "999", IN, OUT).outcome);
        assertEquals(0, payment.charges);
    }

    // --- dates and availability

    @Test
    void pastReversedTooLongOrMissingDatesAreInvalid() {
        assertEquals("INVALID", book(LocalDate.of(2026, 10, 30), LocalDate.of(2026, 11, 2)).outcome);
        assertEquals("INVALID", book(OUT, IN).outcome);
        assertEquals("INVALID", book(IN, IN).outcome);
        assertEquals("INVALID", book(IN, IN.plusDays(31)).outcome);
        assertEquals("INVALID", book(null, OUT).outcome);
        assertEquals(0, payment.charges);
    }

    @Test
    void overlappingDatesAreRejectedWithoutCharging() {
        book(IN, OUT);
        assertEquals("REJECTED", book(IN.plusDays(1), OUT.plusDays(3)).outcome);
        assertEquals("REJECTED", book(IN.minusDays(2), IN.plusDays(1)).outcome);
        assertEquals(1, payment.charges);
    }

    @Test
    void backToBackStaysAreAllowed() {
        book(IN, OUT);
        assertEquals("BOOKED", book(OUT, OUT.plusDays(2)).outcome);
        assertEquals("BOOKED", book(IN.minusDays(2), IN).outcome);
    }

    @Test
    void theRepositoryRefusesASecondHoldEvenIfTheCheckWasPassed() {
        // Two bookings that both passed the availability check before either was saved.
        Reservation first = new Reservation("RES-A", accounts.findById(GUEST).orElseThrow().asGuest(), room, IN, OUT, 160);
        Reservation second = new Reservation("RES-B", accounts.findById(GUEST).orElseThrow().asGuest(), room, IN, OUT, 160);
        reservations.save(first);
        assertThrows(RoomUnavailableException.class, () -> reservations.save(second));
    }

    @Test
    void searchListsOnlyRoomsFreeForTheWholeStay() {
        rooms.save(new Room("204", "Deluxe", 100.0));
        book(IN, OUT);

        List<List<RoomResponse>> seen = new ArrayList<>();
        AvailabilityPresenter presenter = new AvailabilityPresenter() {
            @Override public void presentAvailableRooms(LocalDate in, LocalDate out, int nights, List<RoomResponse> free) { seen.add(free); }
            @Override public void presentInvalidRequest(String message) { seen.add(null); }
        };
        SearchAvailableRoomsUseCase search = new SearchAvailableRoomsInteractor(rooms, reservations, TODAY);
        search.execute(IN.plusDays(1), OUT.plusDays(1), presenter);
        search.execute(OUT, OUT.plusDays(2), presenter);
        search.execute(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3), presenter);

        assertEquals(List.of("204"), seen.get(0).stream().map(RoomResponse::roomNumber).toList());
        assertEquals(List.of("101", "204"), seen.get(1).stream().map(RoomResponse::roomNumber).toList());
        assertEquals(null, seen.get(2), "dates in the past are refused");
    }

    @Test
    void quoteUsesTheShopPriceAndChecksAvailability() {
        List<Object> seen = new ArrayList<>();
        BookingQuotePresenter presenter = new BookingQuotePresenter() {
            @Override public void presentQuote(BookingQuoteResponse quote) { seen.add(quote); }
            @Override public void presentNotFound(String message) { seen.add("NOT_FOUND"); }
            @Override public void presentRejected(String message) { seen.add("REJECTED"); }
            @Override public void presentInvalidRequest(String message) { seen.add("INVALID"); }
        };
        QuoteBookingUseCase quote = new QuoteBookingInteractor(rooms, reservations, shop, TODAY);

        quote.execute(new QuoteRequest("101", IN, OUT, true, false, false), presenter);
        BookingQuoteResponse response = (BookingQuoteResponse) seen.get(0);
        assertEquals(2, response.nights());
        assertEquals(190.0, response.total(), 1e-9);
        assertEquals("Room stay + Breakfast", response.breakdown());

        book(IN, OUT);
        quote.execute(new QuoteRequest("101", IN, OUT, false, false, false), presenter);
        assertEquals("REJECTED", seen.get(1));
    }

    // --- lifecycle and cancellation

    @Test
    void fullStayMovesThroughLifecycleAndRoomStatus() {
        String id = book(IN, OUT).reservation.reservationId();

        assertEquals("CHECKED_IN", checkIn(id).reservation.state());
        assertEquals(RoomStatus.OCCUPIED, room.getStatus());
        assertEquals("COMPLETED", checkOut(id).reservation.state());
        assertEquals(RoomStatus.AVAILABLE, room.getStatus());
        assertEquals("BOOKED", book(IN, OUT).outcome, "a completed stay no longer holds its dates");
    }

    @Test
    void guestCancelsOwnReservationGetsARefundAndFreesTheDates() {
        String id = book(IN, OUT).reservation.reservationId();

        RecordingPresenter cancelled = cancel(id, GUEST);
        assertEquals("UPDATED", cancelled.outcome);
        assertEquals("CANCELLED", cancelled.reservation.state());
        assertEquals(List.of("PAY-1"), payment.refunds);
        assertEquals(List.of("CONFIRMED", "CANCELLED"), events);
        assertEquals("BOOKED", book(IN, OUT).outcome);
    }

    @Test
    void guestCannotCancelSomeoneElsesReservationButAnAdminCan() {
        String id = book(IN, OUT).reservation.reservationId();

        assertEquals("NOT_FOUND", cancel(id, "another-guest").outcome);
        assertTrue(payment.refunds.isEmpty());
        assertEquals("UPDATED", cancel(id, null).outcome);
    }

    @Test
    void checkedInOrCancelledReservationsCannotBeCancelled() {
        String id = book(IN, OUT).reservation.reservationId();
        checkIn(id);
        assertEquals("REJECTED", cancel(id, GUEST).outcome);
        assertTrue(payment.refunds.isEmpty());
    }

    @Test
    void checkOutBeforeCheckInIsRejected() {
        assertEquals("REJECTED", checkOut(book(IN, OUT).reservation.reservationId()).outcome);
    }

    @Test
    void unknownReservationIsNotFound() {
        assertEquals("NOT_FOUND", checkIn("RES-404").outcome);
        assertEquals("NOT_FOUND", checkOut("RES-404").outcome);
        assertEquals("NOT_FOUND", cancel("RES-404", null).outcome);
    }

    @Test
    void listsReservationsForAllOrForOneGuest() {
        accounts.save(new Account("guest-2", "sam", "hash", AccountRole.GUEST, "Sam Lee", "sam@example.com", "+41"));
        book(IN, OUT);
        book("guest-2", "101", OUT, OUT.plusDays(1));

        List<List<ReservationResponse>> seen = new ArrayList<>();
        ListReservationsUseCase list = new ListReservationsInteractor(reservations);
        list.execute(null, seen::add);
        list.execute("guest-2", seen::add);

        assertEquals(2, seen.get(0).size());
        assertEquals("Sam Lee", seen.get(1).get(0).guestName());
    }
}
