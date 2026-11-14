package org.nightking.webapp.controller;

import java.time.LocalDate;
import org.nightking.webapp.model.BookingRequest;
import org.springframework.format.annotation.DateTimeFormat;

/** Backing object of the booking page. */
public class BookingForm {
    private String roomNumber;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkIn;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkOut;
    private boolean breakfast;
    private boolean spa;
    private boolean airportTransfer;

    BookingRequest toRequest(String guestId) {
        return new BookingRequest(guestId, roomNumber, checkIn, checkOut, breakfast, spa, airportTransfer);
    }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
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
}
