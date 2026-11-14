package hotel.services;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
import hotel.model.Guest;
import hotel.model.Reservation;
import hotel.model.Room;

public class ReservationService {
    private final AtomicInteger sequence = new AtomicInteger(1000);

    public Reservation createReservation(Guest guest, Room room, LocalDate checkIn,
                                         LocalDate checkOut, double totalPrice) {
        String id = "RES-" + sequence.getAndIncrement();
        return new Reservation(id, guest, room, checkIn, checkOut, totalPrice);
    }
}
