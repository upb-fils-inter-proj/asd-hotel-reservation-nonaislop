package org.nightking.webapp;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nightking.webapp.gateway.HotelApiException;
import org.nightking.webapp.gateway.HotelManagementGateway;
import org.nightking.webapp.model.Account;
import org.nightking.webapp.model.Availability;
import org.nightking.webapp.model.BookingQuote;
import org.nightking.webapp.model.BookingRequest;
import org.nightking.webapp.model.CardDetails;
import org.nightking.webapp.model.QuoteRequest;
import org.nightking.webapp.model.Registration;
import org.nightking.webapp.model.Reservation;
import org.nightking.webapp.model.Room;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Pages, access rules and the booking flow of the web app, with the hotel management service mocked out. */
@SpringBootTest(properties = "hotel_api_password=unused")
@AutoConfigureMockMvc
class WebAppSecurityTest {
    private static final LocalDate IN = LocalDate.of(2026, 12, 10);
    private static final LocalDate OUT = LocalDate.of(2026, 12, 13);
    private static final Account GUEST = new Account("guest-1", "alex", "GUEST", "Alex Morgan", "alex@example.com", "+40");
    private static final Account ADMIN = new Account("admin-1", "admin", "ADMIN", "Administrator", "admin@hotel.local", "-");
    private static final Room ROOM = new Room("204", "Deluxe", new BigDecimal("100.00"), "EUR", "AVAILABLE");
    private static final Reservation RESERVATION = new Reservation("RES-1000", "guest-1", "Alex Morgan", "204",
            IN, OUT, new BigDecimal("450.00"), "EUR", "CONFIRMED", "PAY-1", "Visa •••• 4242");
    private static final BookingQuote QUOTE = new BookingQuote("204", "Deluxe", IN, OUT, 3,
            new BigDecimal("450.00"), "EUR", "Room stay + Breakfast + Spa package");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private HotelManagementGateway hotel;

    @BeforeEach
    void stubHotel() {
        when(hotel.rooms()).thenReturn(List.of(ROOM));
        when(hotel.availableRooms(IN, OUT)).thenReturn(new Availability(IN, OUT, 3, List.of(ROOM)));
        when(hotel.quote(any())).thenReturn(QUOTE);
        when(hotel.reservationsOf("guest-1")).thenReturn(List.of(RESERVATION));
        when(hotel.allReservations()).thenReturn(List.of(RESERVATION));
    }

    private static RequestPostProcessor signedIn(Account account) {
        return authentication(UsernamePasswordAuthenticationToken.authenticated(account, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + account.role()))));
    }

    // --- public pages

    @Test
    void anyoneCanSeeRoomsAndSearchAvailability() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Check availability")));
        mvc.perform(get("/rooms/available").param("checkIn", "2026-12-10").param("checkOut", "2026-12-13"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("/book/204?checkIn=2026-12-10&amp;checkOut=2026-12-13")));
    }

    @Test
    void invalidSearchShowsTheReason() throws Exception {
        when(hotel.availableRooms(any(), any())).thenThrow(new HotelApiException("Check-in can't be in the past."));
        mvc.perform(get("/rooms/available").param("checkIn", "2020-01-01").param("checkOut", "2020-01-02"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Check-in can&#39;t be in the past.")));
    }

    // --- booking flow

    @Test
    void choosingARoomRequiresLogin() throws Exception {
        mvc.perform(get("/book/204").param("checkIn", "2026-12-10").param("checkOut", "2026-12-13"))
                .andExpect(redirectedUrlPattern("**/login"));
        mvc.perform(post("/book/204/confirm").with(csrf())).andExpect(redirectedUrlPattern("**/login"));
        verify(hotel, never()).book(any());
    }

    @Test
    void guestPicksExtrasThenReviewsThePrice() throws Exception {
        mvc.perform(get("/book/204").with(signedIn(GUEST)).param("checkIn", "2026-12-10").param("checkOut", "2026-12-13"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Additional services")));

        mvc.perform(post("/book/204/review").with(signedIn(GUEST)).with(csrf())
                        .param("checkIn", "2026-12-10").param("checkOut", "2026-12-13").param("breakfast", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Room stay + Breakfast + Spa package")))
                .andExpect(content().string(Matchers.containsString("Pay EUR 450.00")));
        verify(hotel).quote(new QuoteRequest("204", IN, OUT, true, false, false));
    }

    @Test
    void roomTakenMeanwhileSendsTheGuestBackToSearch() throws Exception {
        when(hotel.quote(any())).thenThrow(new HotelApiException("Room 204 is already booked for those dates."));
        mvc.perform(get("/book/204").with(signedIn(GUEST)).param("checkIn", "2026-12-10").param("checkOut", "2026-12-13"))
                .andExpect(redirectedUrl("/rooms/available?checkIn=2026-12-10&checkOut=2026-12-13"))
                .andExpect(flash().attribute("error", "Room 204 is already booked for those dates."));
    }

    @Test
    void payingBooksForTheSignedInGuestWithTheCard() throws Exception {
        when(hotel.book(any())).thenReturn(RESERVATION);

        mvc.perform(post("/book/204/confirm").with(signedIn(GUEST)).with(csrf())
                        .param("guestId", "someone-else")
                        .param("checkIn", "2026-12-10").param("checkOut", "2026-12-13").param("breakfast", "true")
                        .param("cardHolder", "Alex Morgan").param("cardNumber", "4242 4242 4242 4242")
                        .param("expiryMonth", "12").param("expiryYear", "2030").param("cvc", "123"))
                .andExpect(redirectedUrl("/reservations"))
                .andExpect(flash().attribute("message", Matchers.containsString("Payment approved (Visa •••• 4242)")));

        verify(hotel).book(new BookingRequest("guest-1", "204", IN, OUT, true, false, false,
                new CardDetails("Alex Morgan", "4242 4242 4242 4242", 12, 2030, "123")));
    }

    @Test
    void declinedCardReturnsToReviewWithoutEchoingTheCard() throws Exception {
        when(hotel.book(any())).thenThrow(new HotelApiException("Payment declined: Insufficient funds."));

        mvc.perform(post("/book/204/confirm").with(signedIn(GUEST)).with(csrf())
                        .param("checkIn", "2026-12-10").param("checkOut", "2026-12-13")
                        .param("cardHolder", "Alex Morgan").param("cardNumber", "4000000000009995")
                        .param("expiryMonth", "12").param("expiryYear", "2030").param("cvc", "123"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Payment declined: Insufficient funds.")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("value=\"4000000000009995\""))));
    }

    @Test
    void formsNeedACsrfToken() throws Exception {
        mvc.perform(post("/book/204/confirm").with(signedIn(GUEST)).param("checkIn", "2026-12-10"))
                .andExpect(status().isForbidden());
    }

    // --- reservations and administration

    @Test
    void guestSeesAndCancelsOwnReservations() throws Exception {
        when(hotel.cancel("RES-1000", "guest-1")).thenReturn(RESERVATION);

        mvc.perform(get("/reservations").with(signedIn(GUEST)))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Visa •••• 4242")))
                .andExpect(content().string(Matchers.containsString("/reservations/RES-1000/cancel")));
        mvc.perform(post("/reservations/RES-1000/cancel").with(signedIn(GUEST)).with(csrf()))
                .andExpect(redirectedUrl("/reservations"))
                .andExpect(flash().attribute("message", Matchers.containsString("refunded")));
        verify(hotel).cancel("RES-1000", "guest-1");
    }

    @Test
    void declinedAttemptsAreHiddenFromTheGuestsList() throws Exception {
        Reservation declined = new Reservation("RES-0999", "guest-1", "Alex Morgan", "204", IN, OUT,
                new BigDecimal("450.00"), "EUR", "CANCELLED", null, null);
        when(hotel.reservationsOf("guest-1")).thenReturn(List.of(RESERVATION, declined));

        mvc.perform(get("/reservations").with(signedIn(GUEST)))
                .andExpect(content().string(Matchers.containsString("RES-1000")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("RES-0999"))));
    }

    @Test
    void navigationLinksToTheSearch() throws Exception {
        mvc.perform(get("/").with(signedIn(GUEST)))
                .andExpect(content().string(Matchers.containsString("href=\"/#search\"")));
    }

    @Test
    void administrationIsForAdminsOnly() throws Exception {
        mvc.perform(get("/admin").with(signedIn(GUEST))).andExpect(status().isForbidden());
        mvc.perform(get("/admin").with(signedIn(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Check in")));
    }

    @Test
    void adminCancelsAnyReservation() throws Exception {
        when(hotel.cancel("RES-1000", null)).thenReturn(RESERVATION);
        mvc.perform(post("/admin/reservations/RES-1000/cancel").with(signedIn(ADMIN)).with(csrf()))
                .andExpect(redirectedUrl("/admin"));
        verify(hotel).cancel("RES-1000", null);
    }

    @Test
    void guestCannotUseAdminActions() throws Exception {
        mvc.perform(post("/admin/reservations/RES-1000/check-in").with(signedIn(GUEST)).with(csrf()))
                .andExpect(status().isForbidden());
        verify(hotel, never()).checkIn(any());
    }

    // --- accounts

    @Test
    void registrationCreatesAccountThenSendsToLogin() throws Exception {
        mvc.perform(post("/register").with(csrf())
                        .param("username", "alex").param("password", "correct-horse")
                        .param("name", "Alex Morgan").param("email", "alex@example.com").param("phone", "+40"))
                .andExpect(redirectedUrl("/login?registered"));
        verify(hotel).register(new Registration("alex", "correct-horse", "Alex Morgan", "alex@example.com", "+40"));
    }

    @Test
    void loginChecksCredentialsWithTheHotelService() throws Exception {
        when(hotel.authenticate("alex", "correct-horse")).thenReturn(Optional.of(GUEST));
        when(hotel.authenticate("admin", "admin-secret")).thenReturn(Optional.of(ADMIN));
        when(hotel.authenticate(eq("alex"), eq("wrong"))).thenReturn(Optional.empty());

        mvc.perform(post("/login").with(csrf()).param("username", "alex").param("password", "correct-horse"))
                .andExpect(redirectedUrl("/reservations"));
        mvc.perform(post("/login").with(csrf()).param("username", "admin").param("password", "admin-secret"))
                .andExpect(redirectedUrl("/admin"));
        mvc.perform(post("/login").with(csrf()).param("username", "alex").param("password", "wrong"))
                .andExpect(redirectedUrl("/login?error"));
    }
}
