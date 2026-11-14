package org.nightking.hotelmanagement.presenter;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.nightking.hotelmanagement.model.BookingQuoteResponse;
import org.nightking.hotelmanagement.usecase.BookingQuotePresenter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class BookingQuoteWebPresenter implements BookingQuotePresenter {
    private ResponseEntity<Object> response;

    /** JSON shape of a price quote. */
    public record BookingQuoteViewModel(String roomNumber, String roomType, LocalDate checkIn, LocalDate checkOut,
                                        int nights, BigDecimal total, String currency, String breakdown) {}

    @Override
    public void presentQuote(BookingQuoteResponse quote) {
        response = ResponseEntity.ok(new BookingQuoteViewModel(quote.roomNumber(), quote.roomType(), quote.checkIn(),
                quote.checkOut(), quote.nights(), ReservationViewModel.money(quote.total()), "EUR", quote.breakdown()));
    }

    @Override
    public void presentNotFound(String message) {
        response = error(HttpStatus.NOT_FOUND, message);
    }

    @Override
    public void presentRejected(String message) {
        response = error(HttpStatus.CONFLICT, message);
    }

    @Override
    public void presentInvalidRequest(String message) {
        response = error(HttpStatus.BAD_REQUEST, message);
    }

    public ResponseEntity<Object> response() {
        if (response == null) {
            throw new IllegalStateException("The use case did not present a result.");
        }
        return response;
    }

    private static ResponseEntity<Object> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ErrorViewModel(message));
    }
}
