package org.nightking.hotelmanagement.usecase;

import org.nightking.hotelmanagement.model.BookingQuoteResponse;

public interface BookingQuotePresenter {
    void presentQuote(BookingQuoteResponse quote);
    void presentNotFound(String message);
    void presentRejected(String message);
    void presentInvalidRequest(String message);
}
