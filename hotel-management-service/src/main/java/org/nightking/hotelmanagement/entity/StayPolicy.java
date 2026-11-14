package org.nightking.hotelmanagement.entity;

import java.time.LocalDate;

/** Which stay dates the hotel accepts. */
public final class StayPolicy {
    public static final int MAX_NIGHTS = 30;

    private StayPolicy() {
    }

    /** What is wrong with the dates, or null if the hotel accepts them. */
    public static String problemWith(LocalDate checkIn, LocalDate checkOut, LocalDate today) {
        if (checkIn == null || checkOut == null) {
            return "Check-in and check-out dates are required.";
        }
        if (checkIn.isBefore(today)) {
            return "Check-in can't be in the past.";
        }
        if (!checkOut.isAfter(checkIn)) {
            return "Check-out must be after check-in.";
        }
        if (nights(checkIn, checkOut) > MAX_NIGHTS) {
            return "Stays are limited to " + MAX_NIGHTS + " nights.";
        }
        return null;
    }

    public static int nights(LocalDate checkIn, LocalDate checkOut) {
        return (int) (checkOut.toEpochDay() - checkIn.toEpochDay());
    }
}
