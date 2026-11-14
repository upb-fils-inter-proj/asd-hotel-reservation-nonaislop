package org.nightking.hotelmanagement.usecase;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import org.nightking.hotelmanagement.entity.Room;
import org.nightking.hotelmanagement.entity.StayPolicy;
import org.nightking.hotelmanagement.gateway.ReservationRepository;
import org.nightking.hotelmanagement.gateway.RoomRepository;
import org.nightking.hotelmanagement.gateway.ShopGateway;
import org.nightking.hotelmanagement.gateway.ShopGateway.StayQuote;
import org.nightking.hotelmanagement.model.BookingQuoteResponse;
import org.nightking.hotelmanagement.model.QuoteRequest;

public class QuoteBookingInteractor implements QuoteBookingUseCase {
    private final RoomRepository rooms;
    private final RoomAvailability availability;
    private final ShopGateway shop;
    private final Clock clock;

    public QuoteBookingInteractor(RoomRepository rooms, ReservationRepository reservations, ShopGateway shop,
                                  Clock clock) {
        this.rooms = rooms;
        this.availability = new RoomAvailability(reservations);
        this.shop = shop;
        this.clock = clock;
    }

    @Override
    public void execute(QuoteRequest request, BookingQuotePresenter presenter) {
        String problem = StayPolicy.problemWith(request.checkIn(), request.checkOut(), LocalDate.now(clock));
        if (problem != null) {
            presenter.presentInvalidRequest(problem);
            return;
        }
        Optional<Room> found = rooms.findByNumber(request.roomNumber());
        if (found.isEmpty()) {
            presenter.presentNotFound("Room " + request.roomNumber() + " does not exist.");
            return;
        }
        Room room = found.get();
        if (!availability.isFree(room.getRoomNumber(), request.checkIn(), request.checkOut())) {
            presenter.presentRejected("Room " + room.getRoomNumber() + " is already booked for those dates.");
            return;
        }

        int nights = StayPolicy.nights(request.checkIn(), request.checkOut());
        StayQuote quote = shop.quoteStay(room.getNightlyRate(), nights,
                request.breakfast(), request.spa(), request.airportTransfer());
        presenter.presentQuote(new BookingQuoteResponse(room.getRoomNumber(), room.getRoomType(),
                request.checkIn(), request.checkOut(), nights, quote.total(), quote.breakdown()));
    }
}
