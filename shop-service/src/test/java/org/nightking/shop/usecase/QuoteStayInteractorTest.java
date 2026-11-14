package org.nightking.shop.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.nightking.shop.entity.pricing.LongStayDiscountPricing;
import org.nightking.shop.entity.pricing.PricingPolicy;
import org.nightking.shop.entity.pricing.PricingStrategy;
import org.nightking.shop.entity.pricing.SeasonalPricing;
import org.nightking.shop.entity.pricing.StandardPricing;
import org.nightking.shop.gateway.ExtrasCatalog;
import org.nightking.shop.model.StayQuoteRequest;
import org.nightking.shop.model.StayQuoteResponse;

class QuoteStayInteractorTest {
    private static final double DELTA = 1e-9;
    /** Same prices as the shop database seed: breakfast 15/night, spa 45, airport transfer 30. */
    private static final ExtrasCatalog PRICES = extra -> switch (extra) {
        case BREAKFAST -> 15.0;
        case SPA -> 45.0;
        case AIRPORT_TRANSFER -> 30.0;
    };

    static class RecordingPresenter implements QuoteStayPresenter {
        StayQuoteResponse quote;
        String invalidReason;

        @Override public void presentQuote(StayQuoteResponse quote) { this.quote = quote; }
        @Override public void presentInvalidRequest(String reason) { this.invalidReason = reason; }
    }

    private static RecordingPresenter quote(PricingStrategy strategy, double rate, int nights,
                                            boolean breakfast, boolean spa, boolean transfer) {
        RecordingPresenter presenter = new RecordingPresenter();
        new QuoteStayInteractor(new PricingPolicy(strategy), PRICES)
                .execute(new StayQuoteRequest(rate, nights, breakfast, spa, transfer), presenter);
        return presenter;
    }

    @Test
    void standardPricingWithoutExtrasChargesRoomOnly() {
        StayQuoteResponse quote = quote(new StandardPricing(), 100.0, 3, false, false, false).quote;
        assertEquals(300.0, quote.total(), DELTA);
        assertEquals("Room stay", quote.breakdown());
    }

    @Test
    void seasonalPricingWithBreakfastAndSpa() {
        // room 100 * 3 * 1.2 = 360, breakfast 15 * 3 = 45, spa 45
        StayQuoteResponse quote = quote(new SeasonalPricing(1.20), 100.0, 3, true, true, false).quote;
        assertEquals(450.0, quote.total(), DELTA);
        assertEquals("Room stay + Breakfast + Spa package", quote.breakdown());
    }

    @Test
    void airportTransferAddsFlatPrice() {
        StayQuoteResponse quote = quote(new StandardPricing(), 100.0, 2, false, false, true).quote;
        assertEquals(230.0, quote.total(), DELTA);
        assertEquals("Room stay + Airport transfer", quote.breakdown());
    }

    @Test
    void longStayDiscountAppliesFromMinimumNights() {
        assertEquals(450.0, quote(new LongStayDiscountPricing(5, 0.10), 100.0, 5, false, false, false).quote.total(), DELTA);
    }

    @Test
    void longStayDiscountDoesNotApplyBelowMinimumNights() {
        assertEquals(400.0, quote(new LongStayDiscountPricing(5, 0.10), 100.0, 4, false, false, false).quote.total(), DELTA);
    }

    @Test
    void zeroNightsIsAnInvalidRequest() {
        RecordingPresenter presenter = quote(new StandardPricing(), 100.0, 0, false, false, false);
        assertNull(presenter.quote);
        assertNotNull(presenter.invalidReason);
    }
}
