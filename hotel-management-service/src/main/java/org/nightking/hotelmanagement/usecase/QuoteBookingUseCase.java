package org.nightking.hotelmanagement.usecase;

import org.nightking.hotelmanagement.model.QuoteRequest;

/** Prices a stay in a free room, with the shop's breakdown, so the guest can review it before paying. */
public interface QuoteBookingUseCase {
    void execute(QuoteRequest request, BookingQuotePresenter presenter);
}
