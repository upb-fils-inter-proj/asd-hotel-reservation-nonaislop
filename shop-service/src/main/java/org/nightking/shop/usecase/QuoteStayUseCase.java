package org.nightking.shop.usecase;

import org.nightking.shop.model.StayQuoteRequest;

/** Prices a stay: room cost from the active pricing strategy plus any additional services. */
public interface QuoteStayUseCase {
    void execute(StayQuoteRequest request, QuoteStayPresenter presenter);
}
