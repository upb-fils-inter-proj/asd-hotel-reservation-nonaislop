package org.nightking.hotelmanagement.infrastructure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.nightking.hotelmanagement.gateway.ShopGateway;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/** Calls the shop service over HTTP (POST /api/shop/quotes) with HTTP Basic auth. */
public class ShopServiceClient implements ShopGateway {
    private final RestClient restClient;

    public ShopServiceClient(RestClient.Builder builder, String baseUrl, String username, String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "services.shop.password is empty: provide the shop_api_password secret (see README).");
        }
        this.restClient = builder
                .baseUrl(baseUrl)
                .defaultHeaders(headers -> headers.setBasicAuth(username, password))
                .build();
    }

    @Override
    public StayQuote quoteStay(double nightlyRate, int nights, boolean breakfast, boolean spa, boolean airportTransfer) {
        QuoteResponse quote = restClient.post()
                .uri("/api/shop/quotes")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new QuoteRequest(nightlyRate, nights, breakfast, spa, airportTransfer))
                .retrieve()
                .body(QuoteResponse.class);
        if (quote == null) {
            throw new IllegalStateException("Shop service returned an empty quote.");
        }
        return new StayQuote(quote.total(), quote.breakdown());
    }

    /** Wire format of the shop API, owned by this client so the services share no code. */
    record QuoteRequest(double nightlyRate, int nights, boolean breakfast, boolean spa, boolean airportTransfer) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record QuoteResponse(double total, String breakdown) {}
}
