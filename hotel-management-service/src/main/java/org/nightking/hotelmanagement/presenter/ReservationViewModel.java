package org.nightking.hotelmanagement.presenter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import org.nightking.hotelmanagement.model.ReservationResponse;

/** JSON shape of a reservation returned by the API. */
public record ReservationViewModel(String reservationId,
                                   String guestId,
                                   String guestName,
                                   String roomNumber,
                                   LocalDate checkIn,
                                   LocalDate checkOut,
                                   BigDecimal totalPrice,
                                   String currency,
                                   String state,
                                   String paymentReference,
                                   String paidWith) {

    static ReservationViewModel from(ReservationResponse reservation) {
        return new ReservationViewModel(
                reservation.reservationId(),
                reservation.guestId(),
                reservation.guestName(),
                reservation.roomNumber(),
                reservation.checkIn(),
                reservation.checkOut(),
                money(reservation.totalPrice()),
                "EUR",
                reservation.state(),
                reservation.paymentReference(),
                reservation.paidWith());
    }

    static BigDecimal money(double amount) {
        return BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP);
    }
}
