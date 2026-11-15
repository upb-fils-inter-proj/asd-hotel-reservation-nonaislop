package org.nightking.hotelmanagement.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.nightking.hotelmanagement.gateway.ShopGateway;

/** Asks the shop service for the price of a stay, over MQTT (topic shop/quotes/request). */
public class ShopMqttClient implements ShopGateway {
    private final MqttRequester requester;

    public ShopMqttClient(MqttRequester requester) {
        this.requester = requester;
    }

    @Override
    public StayQuote quoteStay(double nightlyRate, int nights, boolean breakfast, boolean spa, boolean airportTransfer) {
        MqttReply reply = requester.request(MqttTopics.SHOP_QUOTE_REQUEST,
                new QuoteRequest(nightlyRate, nights, breakfast, spa, airportTransfer));
        if (!reply.ok()) {
            throw new IllegalStateException("The shop service could not quote the stay: " + reply.error());
        }
        QuoteResponse quote = requester.read(reply, QuoteResponse.class);
        return new StayQuote(quote.total(), quote.breakdown());
    }

    /** Wire format of the shop's quote message, owned by this client so the services share no code. */
    record QuoteRequest(double nightlyRate, int nights, boolean breakfast, boolean spa, boolean airportTransfer) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record QuoteResponse(double total, String breakdown) {}
}
