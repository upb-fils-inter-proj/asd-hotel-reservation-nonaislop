package org.nightking.hotelmanagement.usecase;

import java.util.Optional;
import org.nightking.hotelmanagement.entity.Account;
import org.nightking.hotelmanagement.entity.Reservation;
import org.nightking.hotelmanagement.entity.Room;
import org.nightking.hotelmanagement.gateway.AccountRepository;
import org.nightking.hotelmanagement.gateway.PaymentGateway;
import org.nightking.hotelmanagement.gateway.ReservationRepository;
import org.nightking.hotelmanagement.gateway.RoomRepository;
import org.nightking.hotelmanagement.gateway.ShopGateway;
import org.nightking.hotelmanagement.model.BookRoomRequest;
import org.nightking.hotelmanagement.model.ReservationResponse;

public class BookRoomInteractor implements BookRoomUseCase {
    private final AccountRepository accounts;
    private final RoomRepository rooms;
    private final ReservationRepository reservations;
    private final ShopGateway shop;
    private final PaymentGateway payment;
    private final ReservationEventPublisher eventPublisher;

    public BookRoomInteractor(AccountRepository accounts,
                              RoomRepository rooms,
                              ReservationRepository reservations,
                              ShopGateway shop,
                              PaymentGateway payment,
                              ReservationEventPublisher eventPublisher) {
        this.accounts = accounts;
        this.rooms = rooms;
        this.reservations = reservations;
        this.shop = shop;
        this.payment = payment;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void execute(BookRoomRequest request, ReservationPresenter presenter) {
        if (request.guestId() == null || request.roomNumber() == null
                || request.checkIn() == null || request.checkOut() == null) {
            presenter.presentInvalidRequest("guestId, roomNumber, checkIn and checkOut are required.");
            return;
        }

        int nights = (int) (request.checkOut().toEpochDay() - request.checkIn().toEpochDay());
        if (nights <= 0) {
            presenter.presentInvalidRequest(reject("Check-out must be after check-in."));
            return;
        }

        Optional<Account> account = accounts.findById(request.guestId());
        if (account.isEmpty()) {
            presenter.presentNotFound(reject("Guest account " + request.guestId() + " does not exist. Register first."));
            return;
        }

        Optional<Room> found = rooms.findByNumber(request.roomNumber());
        if (found.isEmpty()) {
            presenter.presentNotFound(reject("Room " + request.roomNumber() + " does not exist."));
            return;
        }
        Room room = found.get();

        boolean taken = reservations.findByRoomNumber(room.getRoomNumber()).stream()
                .anyMatch(existing -> existing.blocks(request.checkIn(), request.checkOut()));
        if (taken) {
            presenter.presentRejected(reject("Room " + room.getRoomNumber() + " is already booked for those dates."));
            return;
        }

        double total = shop.quoteStay(room.getNightlyRate(), nights,
                request.breakfast(), request.spa(), request.airportTransfer());

        Reservation reservation = new Reservation(reservations.nextId(), account.get().asGuest(), room,
                request.checkIn(), request.checkOut(), total);
        reservations.save(reservation);

        if (!payment.pay(reservation.getId(), reservation.getTotalPrice())) {
            System.out.println("Payment failed. Reservation remains pending.");
            presenter.presentBooked(ReservationResponse.from(reservation));
            return;
        }

        reservation.confirm();
        reservations.save(reservation);
        eventPublisher.notifyObservers(reservation, "CONFIRMED");
        presenter.presentBooked(ReservationResponse.from(reservation));
    }

    private static String reject(String message) {
        System.out.println("Booking failed: " + message);
        return message;
    }
}
