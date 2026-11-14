package org.nightking.hotelmanagement.controller;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nightking.hotelmanagement.gateway.PaymentGateway;
import org.nightking.hotelmanagement.gateway.ShopGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** The hotel management API with the shop and payment services mocked out. */
@SpringBootTest(properties = {
        "hotel_api_password=test-secret",
        "shop_api_password=unused",
        "payment_api_password=unused"})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ReservationApiTest {
    private static final String BOOKING = """
            {
              "guest": {"id": "G-01", "name": "Alex Morgan", "email": "alex@example.com", "phone": "+40123456789"},
              "roomNumber": "204",
              "checkIn": "2026-12-10",
              "checkOut": "2026-12-13",
              "breakfast": true,
              "spa": true,
              "airportTransfer": false
            }
            """;

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

    private static MockHttpServletRequestBuilder booking() {
        return authorized(post("/api/reservations").contentType(MediaType.APPLICATION_JSON).content(BOOKING));
    }

    @Test
    void requiresCredentials() throws Exception {
        mvc.perform(post("/api/reservations").contentType(MediaType.APPLICATION_JSON).content(BOOKING))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void bookingReturnsCreatedConfirmedReservation() throws Exception {
        mvc.perform(booking())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reservationId").value("RES-1000"))
                .andExpect(jsonPath("$.roomNumber").value("204"))
                .andExpect(jsonPath("$.totalPrice").value(450.0))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.state").value("CONFIRMED"));
    }

    @Test
    void checkInAndCheckOut() throws Exception {
        mvc.perform(booking()).andExpect(status().isCreated());

        mvc.perform(authorized(post("/api/reservations/RES-1000/check-in")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("CHECKED_IN"));
        mvc.perform(authorized(post("/api/reservations/RES-1000/check-out")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("COMPLETED"));
    }

    @Test
    void declinedPaymentGivesPendingReservationThatCannotCheckIn() throws Exception {
        when(payment.pay(anyString(), anyDouble())).thenReturn(false);

        mvc.perform(booking())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.state").value("PENDING"));
        mvc.perform(authorized(post("/api/reservations/RES-1000/check-in")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void occupiedRoomIsConflict() throws Exception {
        mvc.perform(booking()).andExpect(status().isCreated());
        mvc.perform(authorized(post("/api/reservations/RES-1000/check-in"))).andExpect(status().isOk());

        mvc.perform(booking()).andExpect(status().isConflict());
    }

    @Test
    void unknownReservationIsNotFound() throws Exception {
        mvc.perform(authorized(post("/api/reservations/RES-404/check-in")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Reservation RES-404 not found."));
    }

    @Test
    void reversedDatesAreBadRequest() throws Exception {
        String reversed = BOOKING.replace("2026-12-13", "2026-12-01");
        mvc.perform(authorized(post("/api/reservations").contentType(MediaType.APPLICATION_JSON).content(reversed)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void malformedJsonIsBadRequest() throws Exception {
        mvc.perform(authorized(post("/api/reservations").contentType(MediaType.APPLICATION_JSON).content("{nope")))
                .andExpect(status().isBadRequest());
    }
}
