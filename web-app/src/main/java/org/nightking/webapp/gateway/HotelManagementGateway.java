package org.nightking.webapp.gateway;

import java.util.List;
import java.util.Optional;
import org.nightking.webapp.model.Account;
import org.nightking.webapp.model.BookingRequest;
import org.nightking.webapp.model.Registration;
import org.nightking.webapp.model.Reservation;
import org.nightking.webapp.model.Room;

/**
 * Everything the web app needs from the hotel management service.
 * Business refusals (room taken, username in use, ...) are thrown as {@link HotelApiException}.
 */
public interface HotelManagementGateway {
    List<Room> rooms();

    List<Reservation> allReservations();

    List<Reservation> reservationsOf(String guestId);

    Reservation book(BookingRequest booking);

    Reservation checkIn(String reservationId);

    Reservation checkOut(String reservationId);

    Account register(Registration registration);

    /** Empty when the username or password is wrong. */
    Optional<Account> authenticate(String username, String password);
}
