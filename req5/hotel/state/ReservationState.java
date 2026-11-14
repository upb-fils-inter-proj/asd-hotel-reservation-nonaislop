package hotel.state;

import hotel.model.Reservation;

public interface ReservationState {
    void confirm(Reservation reservation);
    void checkIn(Reservation reservation);
    void checkOut(Reservation reservation);
    void cancel(Reservation reservation);
    String getName();
}
