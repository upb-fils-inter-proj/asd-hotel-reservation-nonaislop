package org.nightking.payment.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.nightking.payment.infrastructure.messaging.MqttTransport;
import org.nightking.payment.infrastructure.persistence.PaymentJpaEntity;
import org.nightking.payment.infrastructure.persistence.PaymentJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest(properties = "payment_api_password=test-secret")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentApiTest {
    @MockitoBean
    private MqttTransport mqtt;


    @Autowired
    private MockMvc mvc;

    @Autowired
    private PaymentJpaRepository payments;

    private static String payment(String reservationId, String cardNumber) {
        return """
                {"reservationId": "%s", "amount": 450.0,
                 "card": {"holderName": "Alex Morgan", "number": "%s", "expiryMonth": 12, "expiryYear": 2030, "cvc": "123"}}
                """.formatted(reservationId, cardNumber);
    }

    private static MockHttpServletRequestBuilder authorized(MockHttpServletRequestBuilder request) {
        return request.with(httpBasic("payment-client", "test-secret"));
    }

    @Test
    void requiresCredentials() throws Exception {
        mvc.perform(post("/api/payments").contentType(MediaType.APPLICATION_JSON).content(payment("RES-1", "4242424242424242")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void approvesAndStoresOnlyTheLastFourDigits() throws Exception {
        String body = mvc.perform(authorized(post("/api/payments")).contentType(MediaType.APPLICATION_JSON)
                        .content(payment("RES-APPROVED", "4242 4242 4242 4242")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.card").value("Visa •••• 4242"))
                .andExpect(jsonPath("$.amount").value(450.0))
                .andReturn().getResponse().getContentAsString();

        String transactionId = JsonPath.read(body, "$.transactionId");
        PaymentJpaEntity stored = payments.findByTransactionId(transactionId).orElseThrow();
        assertEquals("APPROVED", stored.getStatus());
        assertEquals("4242", stored.getCardLast4());
    }

    @Test
    void declinedCardsAreReportedWithAReason() throws Exception {
        mvc.perform(authorized(post("/api/payments")).contentType(MediaType.APPLICATION_JSON)
                        .content(payment("RES-FUNDS", "4000 0000 0000 9995")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DECLINED"))
                .andExpect(jsonPath("$.declineReason").value("Insufficient funds."));
    }

    @Test
    void refundsAnApprovedPaymentOnce() throws Exception {
        String body = mvc.perform(authorized(post("/api/payments")).contentType(MediaType.APPLICATION_JSON)
                        .content(payment("RES-REFUND", "4242424242424242")))
                .andReturn().getResponse().getContentAsString();
        String transactionId = JsonPath.read(body, "$.transactionId");

        mvc.perform(authorized(post("/api/payments/" + transactionId + "/refund")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"));
        mvc.perform(authorized(post("/api/payments/" + transactionId + "/refund")))
                .andExpect(status().isConflict());
        mvc.perform(authorized(post("/api/payments/PAY-NOPE/refund")))
                .andExpect(status().isNotFound());
    }

    @Test
    void missingCardIsBadRequest() throws Exception {
        mvc.perform(authorized(post("/api/payments")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reservationId\": \"RES-1\", \"amount\": 10.0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }
}
