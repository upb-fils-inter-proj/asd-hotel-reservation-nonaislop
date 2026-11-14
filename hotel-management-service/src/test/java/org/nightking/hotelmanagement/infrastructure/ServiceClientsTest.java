package org.nightking.hotelmanagement.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.nightking.hotelmanagement.gateway.PaymentGateway;
import org.nightking.hotelmanagement.gateway.ShopGateway.StayQuote;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Checks the HTTP contract the hotel management service uses to call the shop and payment services. */
class ServiceClientsTest {
    private static final PaymentGateway.Card CARD =
            new PaymentGateway.Card("Alex Morgan", "4242424242424242", 12, 2030, "123");

    private static String basic(String user, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void shopClientPostsQuoteRequestWithBasicAuth() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ShopServiceClient client = new ShopServiceClient(builder, "http://shop-service:8081", "shop-client", "s3cret");

        server.expect(requestTo("http://shop-service:8081/api/shop/quotes"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, basic("shop-client", "s3cret")))
                .andExpect(jsonPath("$.nightlyRate").value(100.0))
                .andExpect(jsonPath("$.nights").value(3))
                .andExpect(jsonPath("$.breakfast").value(true))
                .andRespond(withSuccess("{\"total\": 450.00, \"currency\": \"EUR\", \"breakdown\": \"Room stay + Breakfast\"}",
                        MediaType.APPLICATION_JSON));

        StayQuote quote = client.quoteStay(100.0, 3, true, true, false);
        assertEquals(450.0, quote.total(), 1e-9);
        assertEquals("Room stay + Breakfast", quote.breakdown());
        server.verify();
    }

    @Test
    void paymentClientSendsTheCardAndMapsTheResult() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PaymentServiceClient client = new PaymentServiceClient(builder, "http://payment-service:8082", "payment-client", "s3cret");

        server.expect(requestTo("http://payment-service:8082/api/payments"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, basic("payment-client", "s3cret")))
                .andExpect(jsonPath("$.reservationId").value("RES-1000"))
                .andExpect(jsonPath("$.amount").value(450.0))
                .andExpect(jsonPath("$.card.number").value("4242424242424242"))
                .andExpect(jsonPath("$.card.expiryMonth").value(12))
                .andRespond(withSuccess("""
                        {"transactionId": "PAY-1", "reservationId": "RES-1000", "amount": 450.00, "currency": "EUR",
                         "status": "APPROVED", "declineReason": null, "card": "Visa •••• 4242"}""",
                        MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://payment-service:8082/api/payments"))
                .andRespond(withSuccess("""
                        {"transactionId": "PAY-2", "status": "DECLINED", "declineReason": "Insufficient funds.", "card": "Visa •••• 9995"}""",
                        MediaType.APPLICATION_JSON));

        PaymentGateway.ChargeResult approved = client.charge("RES-1000", 450.0, CARD);
        assertTrue(approved.approved());
        assertEquals("PAY-1", approved.paymentReference());
        assertEquals("Visa •••• 4242", approved.paidWith());

        PaymentGateway.ChargeResult declined = client.charge("RES-1001", 450.0, CARD);
        assertFalse(declined.approved());
        assertEquals("Insufficient funds.", declined.declineReason());
        server.verify();
    }

    @Test
    void paymentClientRefundsByPaymentReference() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PaymentServiceClient client = new PaymentServiceClient(builder, "http://payment-service:8082", "payment-client", "s3cret");

        server.expect(requestTo("http://payment-service:8082/api/payments/PAY-1/refund"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"transactionId\": \"PAY-1\", \"status\": \"REFUNDED\"}", MediaType.APPLICATION_JSON));

        client.refund("PAY-1");
        server.verify();
    }

    @Test
    void cardDetailsAreNeverPrinted() {
        assertFalse(CARD.toString().contains("4242424242424242"));
        assertFalse(CARD.toString().contains("123"));
    }

    @Test
    void clientsRefuseToStartWithoutASecret() {
        assertThrows(IllegalStateException.class,
                () -> new ShopServiceClient(RestClient.builder(), "http://shop-service:8081", "shop-client", ""));
        assertThrows(IllegalStateException.class,
                () -> new PaymentServiceClient(RestClient.builder(), "http://payment-service:8082", "payment-client", null));
    }
}
