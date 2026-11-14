package org.nightking.hotelmanagement.usecase;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import org.nightking.hotelmanagement.entity.Account;
import org.nightking.hotelmanagement.entity.Reservation;
import org.nightking.hotelmanagement.entity.Room;
import org.nightking.hotelmanagement.entity.StayPolicy;
import org.nightking.hotelmanagement.gateway.AccountRepository;
import org.nightking.hotelmanagement.gateway.PaymentGateway;
import org.nightking.hotelmanagement.gateway.PaymentGateway.ChargeResult;
import org.nightking.hotelmanagement.gateway.ReservationRepository;
import org.nightking.hotelmanagement.gateway.RoomRepository;
import org.nightking.hotelmanagement.gateway.RoomUnavailableException;
import org.nightking.hotelmanagement.gateway.ShopGateway;
import org.nightking.hotelmanagement.model.BookRoomRequest;
import org.nightking.hotelmanagement.model.CardDetails;
import org.nightking.hotelmanagement.model.ReservationResponse;

public class BookRoomInteractor implements BookRoomUseCase {
    private final AccountRepository accounts;
    private final RoomRepository rooms;
    private final ReservationRepository reservations;
    private final RoomAvailability availability;
    private final ShopGateway shop;
    private final PaymentGateway payment;
    private final ReservationEventPublisher eventPublisher;
    private final Clock clock;

    public BookRoomInteractor(AccountRepository accounts,
                              RoomRepository rooms,
                              ReservationRepository reservations,
                              ShopGateway shop,
                              PaymentGateway payment,
                              ReservationEventPublisher eventPublisher,
                              Clock clock) {
        this.accounts = accounts;
        this.rooms = rooms;
        this.reservations = reservations;
        this.availability = new RoomAvailability(reservations);
        this.shop = shop;
        this.payment = payment;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Override
    public void execute(BookRoomRequest request, ReservationPresenter presenter) {
        if (request.guestId() == null || request.roomNumber() == null || request.card() == null) {
            presenter.presentInvalidRequest("guestId, roomNumber and card are required.");
            return;
        }
        String dateProblem = StayPolicy.problemWith(request.checkIn(), request.checkOut(), LocalDate.now(clock));
        if (dateProblem != null) {
            presenter.presentInvalidRequest(reject(dateProblem));
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
        if (!availability.isFree(room.getRoomNumber(), request.checkIn(), request.checkOut())) {
            presenter.presentRejected(reject("Room " + room.getRoomNumber() + " is already booked for those dates."));
            return;
        }

        int nights = StayPolicy.nights(request.checkIn(), request.checkOut());
        double total = shop.quoteStay(room.getNightlyRate(), nights,
                request.breakfast(), request.spa(), request.airportTransfer()).total();

        // Hold the room first, so nobody else can take these nights while the card is charged.
        Reservation reservation = new Reservation(reservations.nextId(), account.get().asGuest(), room,
                request.checkIn(), request.checkOut(), total);
        try {
            reservations.save(reservation);
        } catch (RoomUnavailableException e) {
            presenter.presentRejected(reject(e.getMessage()));
            return;
        }

        ChargeResult charge;
        try {
            charge = payment.charge(reservation.getId(), total, toCard(request.card()));
        } catch (RuntimeException e) {
            release(reservation);
            throw e;
        }
        if (!charge.approved()) {
            release(reservation);
            System.out.println("Payment declined for " + reservation.getId() + ": " + charge.declineReason());
            presenter.presentPaymentDeclined(charge.declineReason());
            return;
        }

        reservation.recordPayment(charge.paymentReference(), charge.paidWith());
        reservation.confirm();
        reservations.save(reservation);
        eventPublisher.notifyObservers(reservation, "CONFIRMED");
        presenter.presentBooked(ReservationResponse.from(reservation));
    }

    /** A booking whose payment failed is cancelled, which frees its nights again. */
    private void release(Reservation reservation) {
        reservation.cancel();
        reservations.save(reservation);
    }

    private static PaymentGateway.Card toCard(CardDetails card) {
        return new PaymentGateway.Card(card.holderName(), card.number(), card.expiryMonth(), card.expiryYear(), card.cvc());
    }

    private static String reject(String message) {
        System.out.println("Booking failed: " + message);
        return message;
    }
}
