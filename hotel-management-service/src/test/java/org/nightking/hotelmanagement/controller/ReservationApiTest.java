package org.nightking.hotelmanagement.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nightking.hotelmanagement.gateway.PaymentGateway;
import org.nightking.hotelmanagement.gateway.PaymentGateway.ChargeResult;
import org.nightking.hotelmanagement.gateway.ShopGateway;
import org.nightking.hotelmanagement.gateway.ShopGateway.StayQuote;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
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

    /** "Today" is 1 November 2026 for these tests, so the booking dates stay in the future. */
    @TestConfiguration
    static class FixedClock {
        @Bean
        @Primary
        Clock testClock() {
            return Clock.fixed(Instant.parse("2026-11-01T09:00:00Z"), ZoneOffset.UTC);
        }
    }

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ShopGateway shop;

    @MockitoBean
    private PaymentGateway payment;

    @BeforeEach
    void stubOtherServices() {
        when(shop.quoteStay(anyDouble(), anyInt(), anyBoolean(), anyBoolean(), anyBoolean()))
                .thenReturn(new StayQuote(450.0, "Room stay + Breakfast + Spa package"));
        when(payment.charge(anyString(), anyDouble(), any()))
                .thenReturn(new ChargeResult(true, "PAY-1", "Visa •••• 4242", null));
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
                 "breakfast": true, "spa": true, "airportTransfer": false,
                 "card": {"holderName": "Alex Morgan", "number": "4242424242424242", "expiryMonth": 12, "expiryYear": 2030, "cvc": "123"}}
                """.formatted(guestId, checkIn, checkOut);
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
                .andExpect(jsonPath("$[1].roomNumber").value("204"));
    }

    @Test
    void availabilityHidesBookedRoomsForOverlappingDates() throws Exception {
        String guestId = registerGuest("alex");
        mvc.perform(json(post("/api/reservations"), booking(guestId, "2026-12-10", "2026-12-13")))
                .andExpect(status().isCreated());

        mvc.perform(authorized(get("/api/rooms/available").param("checkIn", "2026-12-12").param("checkOut", "2026-12-14")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nights").value(2))
                .andExpect(jsonPath("$.rooms[*].roomNumber").value(org.hamcrest.Matchers.contains("101", "305")));
        mvc.perform(authorized(get("/api/rooms/available").param("checkIn", "2026-12-13").param("checkOut", "2026-12-14")))
                .andExpect(jsonPath("$.rooms.length()").value(3));
        mvc.perform(authorized(get("/api/rooms/available").param("checkIn", "2026-10-01").param("checkOut", "2026-10-02")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void quoteReturnsTheShopBreakdown() throws Exception {
        mvc.perform(json(post("/api/reservations/quote"), """
                        {"roomNumber": "204", "checkIn": "2026-12-10", "checkOut": "2026-12-13", "breakfast": true, "spa": true}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomType").value("Deluxe"))
                .andExpect(jsonPath("$.nights").value(3))
                .andExpect(jsonPath("$.total").value(450.0))
                .andExpect(jsonPath("$.breakdown").value("Room stay + Breakfast + Spa package"));
    }

    @Test
    void registeredGuestBooksPaysChecksInAndOut() throws Exception {
        String guestId = registerGuest("alex");

        mvc.perform(json(post("/api/reservations"), booking(guestId, "2026-12-10", "2026-12-13")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reservationId").value("RES-1000"))
                .andExpect(jsonPath("$.guestName").value("Alex Morgan"))
                .andExpect(jsonPath("$.totalPrice").value(450.0))
                .andExpect(jsonPath("$.state").value("CONFIRMED"))
                .andExpect(jsonPath("$.paymentReference").value("PAY-1"))
                .andExpect(jsonPath("$.paidWith").value("Visa •••• 4242"));

        mvc.perform(authorized(post("/api/reservations/RES-1000/check-in")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.state").value("CHECKED_IN"));
        mvc.perform(authorized(post("/api/reservations/RES-1000/check-out")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.state").value("COMPLETED"));
    }

    @Test
    void declinedCardIsPaymentRequiredAndBooksNothing() throws Exception {
        when(payment.charge(anyString(), anyDouble(), any()))
                .thenReturn(new ChargeResult(false, "PAY-1", "Visa •••• 9995", "Insufficient funds."));
        String guestId = registerGuest("alex");

        mvc.perform(json(post("/api/reservations"), booking(guestId, "2026-12-10", "2026-12-13")))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.error").value("Payment declined: Insufficient funds."));
        mvc.perform(authorized(get("/api/rooms/available").param("checkIn", "2026-12-10").param("checkOut", "2026-12-13")))
                .andExpect(jsonPath("$.rooms.length()").value(3));
    }

    @Test
    void overlappingBookingIsConflictAndIsNotCharged() throws Exception {
        String guestId = registerGuest("alex");
        mvc.perform(json(post("/api/reservations"), booking(guestId, "2026-12-10", "2026-12-13")))
                .andExpect(status().isCreated());

        mvc.perform(json(post("/api/reservations"), booking(guestId, "2026-12-12", "2026-12-15")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Room 204 is already booked for those dates."));
        verify(payment, org.mockito.Mockito.times(1)).charge(anyString(), anyDouble(), any());
    }

    @Test
    void guestCancelsOwnReservationAndIsRefunded() throws Exception {
        String guestId = registerGuest("alex");
        String other = registerGuest("sam");
        mvc.perform(json(post("/api/reservations"), booking(guestId, "2026-12-10", "2026-12-13")))
                .andExpect(status().isCreated());

        mvc.perform(json(post("/api/reservations/RES-1000/cancel"), "{\"guestId\": \"%s\"}".formatted(other)))
                .andExpect(status().isNotFound());
        verify(payment, never()).refund(anyString());

        mvc.perform(json(post("/api/reservations/RES-1000/cancel"), "{\"guestId\": \"%s\"}".formatted(guestId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("CANCELLED"));
        verify(payment).refund("PAY-1");
        mvc.perform(json(post("/api/reservations"), booking(other, "2026-12-10", "2026-12-13")))
                .andExpect(status().isCreated());
    }

    @Test
    void bookingNeedsARegisteredGuestAndValidDates() throws Exception {
        mvc.perform(json(post("/api/reservations"), booking("not-registered", "2026-12-10", "2026-12-13")))
                .andExpect(status().isNotFound());
        String guestId = registerGuest("alex");
        mvc.perform(json(post("/api/reservations"), booking(guestId, "2026-10-10", "2026-10-13")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Check-in can't be in the past."));
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
    void duplicateUsernameIsConflict() throws Exception {
        registerGuest("alex");
        mvc.perform(json(post("/api/accounts"), """
                        {"username": "alex", "password": "another-pass", "name": "A", "email": "a@b.c", "phone": "1"}"""))
                .andExpect(status().isConflict());
    }

    @Test
    void malformedJsonIsBadRequest() throws Exception {
        mvc.perform(json(post("/api/reservations"), "{nope")).andExpect(status().isBadRequest());
    }
}
