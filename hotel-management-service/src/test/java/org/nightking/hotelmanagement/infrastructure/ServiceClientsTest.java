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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Checks the HTTP contract the hotel management service uses to call the shop and payment services. */
class ServiceClientsTest {

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
                .andExpect(jsonPath("$.spa").value(true))
                .andExpect(jsonPath("$.airportTransfer").value(false))
                .andRespond(withSuccess("{\"total\": 450.00, \"currency\": \"EUR\", \"breakdown\": \"Room stay\"}",
                        MediaType.APPLICATION_JSON));

        assertEquals(450.0, client.quoteStay(100.0, 3, true, true, false), 1e-9);
        server.verify();
    }

    @Test
    void paymentClientMapsApprovedAndDeclined() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PaymentServiceClient client = new PaymentServiceClient(builder, "http://payment-service:8082", "payment-client", "s3cret");

        server.expect(requestTo("http://payment-service:8082/api/payments"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, basic("payment-client", "s3cret")))
                .andExpect(jsonPath("$.reservationId").value("RES-1000"))
                .andExpect(jsonPath("$.amount").value(450.0))
                .andRespond(withSuccess("{\"reservationId\": \"RES-1000\", \"amount\": 450.00, \"status\": \"APPROVED\"}",
                        MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://payment-service:8082/api/payments"))
                .andRespond(withSuccess("{\"reservationId\": \"RES-1001\", \"amount\": 0, \"status\": \"DECLINED\"}",
                        MediaType.APPLICATION_JSON));

        assertTrue(client.pay("RES-1000", 450.0));
        assertFalse(client.pay("RES-1001", 0.0));
        server.verify();
    }

    @Test
    void clientsRefuseToStartWithoutASecret() {
        assertThrows(IllegalStateException.class,
                () -> new ShopServiceClient(RestClient.builder(), "http://shop-service:8081", "shop-client", ""));
        assertThrows(IllegalStateException.class,
                () -> new PaymentServiceClient(RestClient.builder(), "http://payment-service:8082", "payment-client", null));
    }
}
