package org.nightking.hotelmanagement.infrastructure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.nightking.hotelmanagement.gateway.PaymentGateway;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/** Calls the payment service over HTTP (charge and refund) with HTTP Basic auth. */
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
    public ChargeResult charge(String reservationId, double amount, Card card) {
        PaymentResult result = restClient.post()
                .uri("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new PaymentRequest(reservationId, amount, new CardPayload(card.holderName(), card.number(),
                        card.expiryMonth(), card.expiryYear(), card.cvc())))
                .retrieve()
                .body(PaymentResult.class);
        if (result == null) {
            throw new IllegalStateException("Payment service returned an empty result.");
        }
        return new ChargeResult("APPROVED".equals(result.status()), result.transactionId(), result.card(),
                result.declineReason());
    }

    @Override
    public void refund(String paymentReference) {
        restClient.post().uri("/api/payments/{transactionId}/refund", paymentReference).retrieve().toBodilessEntity();
    }

    /** Wire format of the payment API, owned by this client so the services share no code. */
    record PaymentRequest(String reservationId, double amount, CardPayload card) {}

    record CardPayload(String holderName, String number, int expiryMonth, int expiryYear, String cvc) {
        @Override
        public String toString() { return "CardPayload[hidden]"; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PaymentResult(String transactionId, String status, String declineReason, String card) {}
}
