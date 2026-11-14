package org.nightking.webapp.gateway;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.nightking.webapp.model.Account;
import org.nightking.webapp.model.Availability;
import org.nightking.webapp.model.BookingQuote;
import org.nightking.webapp.model.BookingRequest;
import org.nightking.webapp.model.QuoteRequest;
import org.nightking.webapp.model.Registration;
import org.nightking.webapp.model.Reservation;
import org.nightking.webapp.model.Room;

/**
 * Everything the web app needs from the hotel management service.
 * Business refusals (room taken, card declined, username in use, ...) are thrown as {@link HotelApiException}.
 */
public interface HotelManagementGateway {
    List<Room> rooms();

    Availability availableRooms(LocalDate checkIn, LocalDate checkOut);

    BookingQuote quote(QuoteRequest request);

    Reservation book(BookingRequest booking);

    /** guestIdOrNull: the signed-in guest, who may only cancel their own reservations; null for administrators. */
    Reservation cancel(String reservationId, String guestIdOrNull);

    List<Reservation> allReservations();

    List<Reservation> reservationsOf(String guestId);

    Reservation checkIn(String reservationId);

    Reservation checkOut(String reservationId);

    Account register(Registration registration);

    /** Empty when the username or password is wrong. */
    Optional<Account> authenticate(String username, String password);
}
