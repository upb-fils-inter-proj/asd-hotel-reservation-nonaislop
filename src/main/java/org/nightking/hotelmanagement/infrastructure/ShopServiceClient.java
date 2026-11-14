package org.nightking.hotelmanagement.infrastructure;

import org.nightking.hotelmanagement.gateway.ShopGateway;
import org.nightking.shop.controller.ShopController;
import org.nightking.shop.model.StayQuoteRequest;

/**
 * Reaches the shop service through its controller. This is the only class in hotel management
 * that knows the shop exists, so moving the shop behind HTTP only changes this class.
 */
public class ShopServiceClient implements ShopGateway {
    private final ShopController shop;

    public ShopServiceClient(ShopController shop) {
        this.shop = shop;
    }

    @Override
    public double quoteStay(double nightlyRate, int nights,
                            boolean breakfast, boolean spa, boolean airportTransfer) {
        return shop.quoteStay(new StayQuoteRequest(nightlyRate, nights, breakfast, spa, airportTransfer))
                .total();
    }
}
