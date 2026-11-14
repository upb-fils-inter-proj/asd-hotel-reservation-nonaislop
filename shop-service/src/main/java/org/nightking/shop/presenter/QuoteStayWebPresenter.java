package org.nightking.shop.presenter;

import org.nightking.shop.model.StayQuoteResponse;
import org.nightking.shop.usecase.QuoteStayPresenter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** Turns the use case result into an HTTP response. One instance per request. */
public class QuoteStayWebPresenter implements QuoteStayPresenter {
    private ResponseEntity<Object> response;

    @Override
    public void presentQuote(StayQuoteResponse quote) {
        response = ResponseEntity.ok(StayQuoteViewModel.from(quote));
    }

    @Override
    public void presentInvalidRequest(String reason) {
        response = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorViewModel(reason));
    }

    public ResponseEntity<Object> response() {
        if (response == null) {
            throw new IllegalStateException("The use case did not present a result.");
        }
        return response;
    }
}
