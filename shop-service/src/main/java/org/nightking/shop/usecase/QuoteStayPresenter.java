package org.nightking.shop.usecase;

import org.nightking.shop.model.StayQuoteResponse;

/** Output port of the quote-stay use case: how the result leaves the use case. */
public interface QuoteStayPresenter {
    void presentQuote(StayQuoteResponse quote);
    void presentInvalidRequest(String reason);
}
