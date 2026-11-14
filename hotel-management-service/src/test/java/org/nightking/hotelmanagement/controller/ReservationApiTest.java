package org.nightking.hotelmanagement.controller;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nightking.hotelmanagement.gateway.PaymentGateway;
import org.nightking.hotelmanagement.gateway.ShopGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** The hotel management API on a real (H2) database with Flyway migrations; shop and payment are mocked. */
@SpringBootTest(properties = {
        "hotel_api_password=test-secret",
        "shop_api_password=unused",
        "payment_api_password=unused",
        "admin_password=admin-secret"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ReservationApiTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ShopGateway shop;

    @MockitoBean
    private PaymentGateway payment;

    @BeforeEach
    void stubOtherServices() {
        when(shop.quoteStay(anyDouble(), anyInt(), anyBoolean(), anyBoolean(), anyBoolean())).thenReturn(450.0);
        when(payment.pay(anyString(), anyDouble())).thenReturn(true);
    }

    private static MockHttpServletRequestBuilder authorized(MockHttpServletRequestBuilder request) {
        return request.with(httpBasic("front-desk", "test-secret"));
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return authorized(request).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private String registerGuest(String username) throws Exception {
        String body = mvc.perform(json(post("/api/accounts"), """
                        {"username": "%s", "password": "correct-horse", "name": "Alex Morgan",
                         "email": "alex@example.com", "phone": "+40123456789"}""".formatted(username)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private static String booking(String guestId, String checkIn, String checkOut) {
        return """
                {"guestId": "%s", "roomNumber": "204", "checkIn": "%s", "checkOut": "%s",
                 "breakfast": true, "spa": true, "airportTransfer": false}""".formatted(guestId, checkIn, checkOut);
    }

    @Test
    void requiresServiceCredentials() throws Exception {
        mvc.perform(get("/api/rooms")).andExpect(status().isUnauthorized());
    }

    @Test
    void listsSeededRooms() throws Exception {
        mvc.perform(authorized(get("/api/rooms")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[1].roomNumber").value("204"))
                .andExpect(jsonPath("$[1].nightlyRate").value(100.0));
    }

    @Test
    void registeredGuestBooksChecksInAndOut() throws Exception {
        String guestId = registerGuest("alex");

        mvc.perform(json(post("/api/reservations"), booking(guestId, "2026-12-10", "2026-12-13")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reservationId").value("RES-1000"))
                .andExpect(jsonPath("$.guestId").value(guestId))
                .andExpect(jsonPath("$.guestName").value("Alex Morgan"))
                .andExpect(jsonPath("$.totalPrice").value(450.0))
                .andExpect(jsonPath("$.state").value("CONFIRMED"));

        mvc.perform(authorized(post("/api/reservations/RES-1000/check-in")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.state").value("CHECKED_IN"));
        mvc.perform(authorized(get("/api/rooms")))
                .andExpect(jsonPath("$[1].status").value("OCCUPIED"));
        mvc.perform(authorized(post("/api/reservations/RES-1000/check-out")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.state").value("COMPLETED"));
        mvc.perform(authorized(get("/api/rooms")))
                .andExpect(jsonPath("$[1].status").value("AVAILABLE"));
    }

    @Test
    void bookingNeedsARegisteredGuest() throws Exception {
        mvc.perform(json(post("/api/reservations"), booking("not-registered", "2026-12-10", "2026-12-13")))
                .andExpect(status().isNotFound());
    }

    @Test
    void overlappingBookingIsConflict() throws Exception {
        String guestId = registerGuest("alex");
        mvc.perform(json(post("/api/reservations"), booking(guestId, "2026-12-10", "2026-12-13")))
                .andExpect(status().isCreated());

        mvc.perform(json(post("/api/reservations"), booking(guestId, "2026-12-12", "2026-12-15")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Room 204 is already booked for those dates."));
    }

    @Test
    void declinedPaymentGivesPendingReservationThatCannotCheckIn() throws Exception {
        when(payment.pay(anyString(), anyDouble())).thenReturn(false);
        String guestId = registerGuest("alex");

        mvc.perform(json(post("/api/reservations"), booking(guestId, "2026-12-10", "2026-12-13")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.state").value("PENDING"));
        mvc.perform(authorized(post("/api/reservations/RES-1000/check-in")))
                .andExpect(status().isConflict());
    }

    @Test
    void listsReservationsForAllOrOneGuest() throws Exception {
        String alex = registerGuest("alex");
        String sam = registerGuest("sam");
        mvc.perform(json(post("/api/reservations"), booking(alex, "2026-12-01", "2026-12-03"))).andExpect(status().isCreated());
        mvc.perform(json(post("/api/reservations"), booking(sam, "2026-12-05", "2026-12-07"))).andExpect(status().isCreated());

        mvc.perform(authorized(get("/api/reservations")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(authorized(get("/api/reservations").param("guestId", sam)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].guestId").value(sam));
    }

    @Test
    void duplicateUsernameIsConflict() throws Exception {
        registerGuest("alex");
        mvc.perform(json(post("/api/accounts"), """
                        {"username": "alex", "password": "another-pass", "name": "A", "email": "a@b.c", "phone": "1"}"""))
                .andExpect(status().isConflict());
    }

    @Test
    void authenticatesGuestsAndThePreCreatedAdmin() throws Exception {
        registerGuest("alex");
        mvc.perform(json(post("/api/accounts/authenticate"), "{\"username\": \"alex\", \"password\": \"correct-horse\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("GUEST"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        mvc.perform(json(post("/api/accounts/authenticate"), "{\"username\": \"alex\", \"password\": \"nope-nope\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(json(post("/api/accounts/authenticate"), "{\"username\": \"admin\", \"password\": \"admin-secret\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void unknownReservationIsNotFound() throws Exception {
        mvc.perform(authorized(post("/api/reservations/RES-404/check-in")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Reservation RES-404 not found."));
    }

    @Test
    void malformedJsonIsBadRequest() throws Exception {
        mvc.perform(json(post("/api/reservations"), "{nope")).andExpect(status().isBadRequest());
    }
}
