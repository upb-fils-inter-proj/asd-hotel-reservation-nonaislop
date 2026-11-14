package org.nightking.hotelmanagement.infrastructure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.nightking.hotelmanagement.gateway.PaymentGateway;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/** Calls the payment service over HTTP (POST /api/payments) with HTTP Basic auth. */
public class PaymentServiceClient implements PaymentGateway {
    private final RestClient restClient;

    public PaymentServiceClient(RestClient.Builder builder, String baseUrl, String username, String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "services.payment.password is empty: provide the payment_api_password secret (see README).");
        }
        this.restClient = builder
                .baseUrl(baseUrl)
                .defaultHeaders(headers -> headers.setBasicAuth(username, password))
                .build();
    }

    @Override
    public boolean pay(String reservationId, double amount) {
        PaymentResult result = restClient.post()
                .uri("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new PaymentRequest(reservationId, amount))
                .retrieve()
                .body(PaymentResult.class);
        return result != null && "APPROVED".equals(result.status());
    }

    /** Wire format of the payment API, owned by this client so the services share no code. */
    record PaymentRequest(String reservationId, double amount) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PaymentResult(String status) {}
}
