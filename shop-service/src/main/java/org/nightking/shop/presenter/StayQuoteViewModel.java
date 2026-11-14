package org.nightking.shop.presenter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.nightking.shop.model.StayQuoteResponse;

/** JSON shape of a quote returned by the API. */
public record StayQuoteViewModel(BigDecimal total, String currency, String breakdown) {

    static StayQuoteViewModel from(StayQuoteResponse quote) {
        return new StayQuoteViewModel(
                BigDecimal.valueOf(quote.total()).setScale(2, RoundingMode.HALF_UP), "EUR", quote.breakdown());
    }
}
