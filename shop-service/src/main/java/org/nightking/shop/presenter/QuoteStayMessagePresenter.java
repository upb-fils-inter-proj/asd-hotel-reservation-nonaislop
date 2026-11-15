package org.nightking.shop.presenter;

import org.nightking.shop.model.StayQuoteResponse;
import org.nightking.shop.usecase.QuoteStayPresenter;

/** Turns the use case result into an MQTT reply. One instance per request. */
public class QuoteStayMessagePresenter implements QuoteStayPresenter {
    private MessageReply reply;

    @Override
    public void presentQuote(StayQuoteResponse quote) {
        reply = MessageReply.ok(StayQuoteViewModel.from(quote));
    }

    @Override
    public void presentInvalidRequest(String reason) {
        reply = MessageReply.invalidRequest(reason);
    }

    public MessageReply reply() {
        if (reply == null) {
            throw new IllegalStateException("The use case did not present a result.");
        }
        return reply;
    }
}
