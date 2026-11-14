package org.nightking.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.nightking.shop.controller.ShopController;
import org.nightking.shop.entity.pricing.LongStayDiscountPricing;
import org.nightking.shop.entity.pricing.PricingPolicy;
import org.nightking.shop.entity.pricing.PricingStrategy;
import org.nightking.shop.entity.pricing.SeasonalPricing;
import org.nightking.shop.entity.pricing.StandardPricing;
import org.nightking.shop.model.StayQuoteRequest;
import org.nightking.shop.model.StayQuoteResponse;
import org.nightking.shop.usecase.QuoteStayInteractor;

class ShopControllerTest {
    private static final double DELTA = 1e-9;

    private static StayQuoteResponse quote(PricingStrategy strategy, int nights,
                                           boolean breakfast, boolean spa, boolean transfer) {
        ShopController shop = new ShopController(new QuoteStayInteractor(new PricingPolicy(strategy)));
        return shop.quoteStay(new StayQuoteRequest(100.0, nights, breakfast, spa, transfer));
    }

    @Test
    void standardPricingWithoutExtrasChargesRoomOnly() {
        StayQuoteResponse quote = quote(new StandardPricing(), 3, false, false, false);
        assertEquals(300.0, quote.total(), DELTA);
        assertEquals("Room stay", quote.breakdown());
    }

    @Test
    void seasonalPricingWithBreakfastAndSpa() {
        // room 100 * 3 * 1.2 = 360, breakfast 15 * 3 = 45, spa 45
        StayQuoteResponse quote = quote(new SeasonalPricing(1.20), 3, true, true, false);
        assertEquals(450.0, quote.total(), DELTA);
        assertEquals("Room stay + Breakfast + Spa package", quote.breakdown());
    }

    @Test
    void airportTransferAddsFlatPrice() {
        StayQuoteResponse quote = quote(new StandardPricing(), 2, false, false, true);
        assertEquals(230.0, quote.total(), DELTA);
        assertEquals("Room stay + Airport transfer", quote.breakdown());
    }

    @Test
    void longStayDiscountAppliesFromMinimumNights() {
        assertEquals(450.0, quote(new LongStayDiscountPricing(5, 0.10), 5, false, false, false).total(), DELTA);
    }

    @Test
    void longStayDiscountDoesNotApplyBelowMinimumNights() {
        assertEquals(400.0, quote(new LongStayDiscountPricing(5, 0.10), 4, false, false, false).total(), DELTA);
    }
}
