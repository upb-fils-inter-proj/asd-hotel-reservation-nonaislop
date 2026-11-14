package org.nightking.payment.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.nightking.payment.infrastructure.persistence.PaymentJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "payment_api_password=test-secret")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PaymentJpaRepository payments;

    @Test
    void requiresCredentials() throws Exception {
        mvc.perform(post("/api/payments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reservationId\": \"RES-1\", \"amount\": 450.0}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void approvesPayment() throws Exception {
        mvc.perform(post("/api/payments").with(httpBasic("payment-client", "test-secret"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reservationId\": \"RES-APPROVED\", \"amount\": 450.0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservationId").value("RES-APPROVED"))
                .andExpect(jsonPath("$.amount").value(450.0))
                .andExpect(jsonPath("$.status").value("APPROVED"));

        assertEquals("APPROVED", payments.findByReservationId("RES-APPROVED").get(0).getStatus(), "payment is stored in the payment database");
    }

    @Test
    void declinesZeroAmount() throws Exception {
        mvc.perform(post("/api/payments").with(httpBasic("payment-client", "test-secret"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reservationId\": \"RES-1\", \"amount\": 0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DECLINED"));
    }

    @Test
    void missingReservationIdReturnsBadRequest() throws Exception {
        mvc.perform(post("/api/payments").with(httpBasic("payment-client", "test-secret"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\": 10.0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }
}
