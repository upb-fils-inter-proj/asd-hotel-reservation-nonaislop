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
import org.nightking.webapp.model.BookingRequest;
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

/** Pages, access rules and forms of the web app, with the hotel management service mocked out. */
@SpringBootTest(properties = "hotel_api_password=unused")
@AutoConfigureMockMvc
class WebAppSecurityTest {
    private static final Account GUEST = new Account("guest-1", "alex", "GUEST", "Alex Morgan", "alex@example.com", "+40");
    private static final Account ADMIN = new Account("admin-1", "admin", "ADMIN", "Administrator", "admin@hotel.local", "-");
    private static final Room ROOM = new Room("204", "Deluxe", new BigDecimal("100.00"), "EUR", "AVAILABLE");
    private static final Reservation RESERVATION = new Reservation("RES-1000", "guest-1", "Alex Morgan", "204",
            LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 13), new BigDecimal("450.00"), "EUR", "CONFIRMED");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private HotelManagementGateway hotel;

    @BeforeEach
    void stubHotel() {
        when(hotel.rooms()).thenReturn(List.of(ROOM));
        when(hotel.reservationsOf("guest-1")).thenReturn(List.of(RESERVATION));
        when(hotel.allReservations()).thenReturn(List.of(RESERVATION));
    }

    private static RequestPostProcessor signedIn(Account account) {
        return authentication(UsernamePasswordAuthenticationToken.authenticated(account, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + account.role()))));
    }

    @Test
    void anyoneCanSeeTheRooms() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Deluxe")))
                .andExpect(content().string(Matchers.containsString("to book a room")));
    }

    @Test
    void bookingRequiresLogin() throws Exception {
        mvc.perform(get("/reservations/new")).andExpect(redirectedUrlPattern("**/login"));
        mvc.perform(post("/reservations").with(csrf())).andExpect(redirectedUrlPattern("**/login"));
        verify(hotel, never()).book(any());
    }

    @Test
    void guestSeesOwnReservationsAndBookingForm() throws Exception {
        mvc.perform(get("/reservations").with(signedIn(GUEST)))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("RES-1000")));
        mvc.perform(get("/reservations/new").with(signedIn(GUEST)))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Book and pay")));
    }

    @Test
    void bookingUsesTheSignedInGuestNotTheForm() throws Exception {
        when(hotel.book(any())).thenReturn(RESERVATION);

        mvc.perform(post("/reservations").with(signedIn(GUEST)).with(csrf())
                        .param("guestId", "someone-else")
                        .param("roomNumber", "204")
                        .param("checkIn", "2026-12-10")
                        .param("checkOut", "2026-12-13")
                        .param("breakfast", "true"))
                .andExpect(redirectedUrl("/reservations"))
                .andExpect(flash().attribute("message", Matchers.containsString("RES-1000")));

        verify(hotel).book(new BookingRequest("guest-1", "204", LocalDate.of(2026, 12, 10),
                LocalDate.of(2026, 12, 13), true, false, false));
    }

    @Test
    void refusedBookingShowsTheReason() throws Exception {
        when(hotel.book(any())).thenThrow(new HotelApiException("Room 204 is already booked for those dates."));

        mvc.perform(post("/reservations").with(signedIn(GUEST)).with(csrf())
                        .param("roomNumber", "204").param("checkIn", "2026-12-10").param("checkOut", "2026-12-13"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("already booked for those dates")));
    }

    @Test
    void formsNeedACsrfToken() throws Exception {
        mvc.perform(post("/reservations").with(signedIn(GUEST)).param("roomNumber", "204"))
                .andExpect(status().isForbidden());
    }

    @Test
    void administrationIsForAdminsOnly() throws Exception {
        mvc.perform(get("/admin").with(signedIn(GUEST))).andExpect(status().isForbidden());
        mvc.perform(get("/admin").with(signedIn(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Check in")));
    }

    @Test
    void adminChecksAGuestIn() throws Exception {
        when(hotel.checkIn("RES-1000")).thenReturn(RESERVATION);
        mvc.perform(post("/admin/reservations/RES-1000/check-in").with(signedIn(ADMIN)).with(csrf()))
                .andExpect(redirectedUrl("/admin"));
        verify(hotel).checkIn("RES-1000");
    }

    @Test
    void guestCannotUseAdminActions() throws Exception {
        mvc.perform(post("/admin/reservations/RES-1000/check-in").with(signedIn(GUEST)).with(csrf()))
                .andExpect(status().isForbidden());
        verify(hotel, never()).checkIn(any());
    }

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
