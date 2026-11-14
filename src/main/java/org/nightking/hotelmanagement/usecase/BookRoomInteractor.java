package org.nightking.hotelmanagement.usecase;

import java.util.Optional;
import org.nightking.hotelmanagement.entity.Guest;
import org.nightking.hotelmanagement.entity.Reservation;
import org.nightking.hotelmanagement.entity.Room;
import org.nightking.hotelmanagement.gateway.PaymentGateway;
import org.nightking.hotelmanagement.gateway.ReservationRepository;
import org.nightking.hotelmanagement.gateway.RoomRepository;
import org.nightking.hotelmanagement.gateway.ShopGateway;
import org.nightking.hotelmanagement.model.BookRoomRequest;
import org.nightking.hotelmanagement.model.GuestDetails;
import org.nightking.hotelmanagement.model.ReservationResponse;

public class BookRoomInteractor implements BookRoomUseCase {
    private final RoomRepository rooms;
    private final ReservationRepository reservations;
    private final ShopGateway shop;
    private final PaymentGateway payment;
    private final ReservationEventPublisher eventPublisher;

    public BookRoomInteractor(RoomRepository rooms,
                              ReservationRepository reservations,
                              ShopGateway shop,
                              PaymentGateway payment,
                              ReservationEventPublisher eventPublisher) {
        this.rooms = rooms;
        this.reservations = reservations;
        this.shop = shop;
        this.payment = payment;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Optional<ReservationResponse> execute(BookRoomRequest request) {
        Optional<Room> found = rooms.findByNumber(request.roomNumber());
        if (found.isEmpty()) {
            System.out.println("Booking failed: room " + request.roomNumber() + " does not exist.");
            return Optional.empty();
        }
        Room room = found.get();

        if (!room.isAvailable()) {
            System.out.println("Booking failed: room is not available.");
            return Optional.empty();
        }

        int nights = (int) (request.checkOut().toEpochDay() - request.checkIn().toEpochDay());
        if (nights <= 0) {
            System.out.println("Booking failed: check-out must be after check-in.");
            return Optional.empty();
        }

        double total = shop.quoteStay(room.getNightlyRate(), nights,
                request.breakfast(), request.spa(), request.airportTransfer());

        GuestDetails details = request.guest();
        Guest guest = new Guest(details.id(), details.name(), details.email(), details.phone());
        Reservation reservation = new Reservation(reservations.nextId(), guest, room,
                request.checkIn(), request.checkOut(), total);
        reservations.save(reservation);

        if (!payment.pay(reservation.getId(), reservation.getTotalPrice())) {
            System.out.println("Payment failed. Reservation remains pending.");
            return Optional.of(ReservationResponse.from(reservation));
        }

        reservation.confirm();
        reservations.save(reservation);
        eventPublisher.notifyObservers(reservation, "CONFIRMED");
        return Optional.of(ReservationResponse.from(reservation));
    }
}
