package org.nightking.shop.controller;

import org.nightking.shop.model.StayQuoteRequest;
import org.nightking.shop.model.StayQuoteResponse;
import org.nightking.shop.usecase.QuoteStayUseCase;

/** Entry point of the shop service: pricing and additional services. */
public class ShopController {
    private final QuoteStayUseCase quoteStay;

    public ShopController(QuoteStayUseCase quoteStay) {
        this.quoteStay = quoteStay;
    }

    public StayQuoteResponse quoteStay(StayQuoteRequest request) {
        return quoteStay.execute(request);
    }
}
