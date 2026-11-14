package org.nightking.shop.usecase;

import org.nightking.shop.model.StayQuoteRequest;
import org.nightking.shop.model.StayQuoteResponse;

/** Prices a stay: room cost from the active pricing strategy plus any additional services. */
public interface QuoteStayUseCase {
    StayQuoteResponse execute(StayQuoteRequest request);
}
