package org.nightking.webapp.controller;

import java.time.LocalDate;
import org.nightking.webapp.model.BookingRequest;
import org.nightking.webapp.model.CardDetails;
import org.nightking.webapp.model.QuoteRequest;
import org.springframework.format.annotation.DateTimeFormat;

/** Backing object of the booking pages: stay, extras and (on the review page) the card. */
public class BookingForm {
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkIn;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkOut;
    private boolean breakfast;
    private boolean spa;
    private boolean airportTransfer;

    private String cardHolder;
    private String cardNumber;
    private Integer expiryMonth;
    private Integer expiryYear;
    private String cvc;

    QuoteRequest toQuote(String roomNumber) {
        return new QuoteRequest(roomNumber, checkIn, checkOut, breakfast, spa, airportTransfer);
    }

    BookingRequest toBooking(String guestId, String roomNumber) {
        CardDetails card = new CardDetails(cardHolder, cardNumber,
                expiryMonth == null ? 0 : expiryMonth, expiryYear == null ? 0 : expiryYear, cvc);
        return new BookingRequest(guestId, roomNumber, checkIn, checkOut, breakfast, spa, airportTransfer, card);
    }

    /** Card details are never sent back to the browser. */
    void forgetCard() {
        cardNumber = null;
        cvc = null;
    }

    public LocalDate getCheckIn() { return checkIn; }
    public void setCheckIn(LocalDate checkIn) { this.checkIn = checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public void setCheckOut(LocalDate checkOut) { this.checkOut = checkOut; }
    public boolean isBreakfast() { return breakfast; }
    public void setBreakfast(boolean breakfast) { this.breakfast = breakfast; }
    public boolean isSpa() { return spa; }
    public void setSpa(boolean spa) { this.spa = spa; }
    public boolean isAirportTransfer() { return airportTransfer; }
    public void setAirportTransfer(boolean airportTransfer) { this.airportTransfer = airportTransfer; }
    public String getCardHolder() { return cardHolder; }
    public void setCardHolder(String cardHolder) { this.cardHolder = cardHolder; }
    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
    public Integer getExpiryMonth() { return expiryMonth; }
    public void setExpiryMonth(Integer expiryMonth) { this.expiryMonth = expiryMonth; }
    public Integer getExpiryYear() { return expiryYear; }
    public void setExpiryYear(Integer expiryYear) { this.expiryYear = expiryYear; }
    public String getCvc() { return cvc; }
    public void setCvc(String cvc) { this.cvc = cvc; }
}
