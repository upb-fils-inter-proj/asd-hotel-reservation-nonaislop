package org.nightking.hotelmanagement.usecase;

import java.time.Clock;
import java.time.LocalDate;
import org.nightking.hotelmanagement.entity.StayPolicy;
import org.nightking.hotelmanagement.gateway.ReservationRepository;
import org.nightking.hotelmanagement.gateway.RoomRepository;
import org.nightking.hotelmanagement.model.RoomResponse;

public class SearchAvailableRoomsInteractor implements SearchAvailableRoomsUseCase {
    private final RoomRepository rooms;
    private final RoomAvailability availability;
    private final Clock clock;

    public SearchAvailableRoomsInteractor(RoomRepository rooms, ReservationRepository reservations, Clock clock) {
        this.rooms = rooms;
        this.availability = new RoomAvailability(reservations);
        this.clock = clock;
    }

    @Override
    public void execute(LocalDate checkIn, LocalDate checkOut, AvailabilityPresenter presenter) {
        String problem = StayPolicy.problemWith(checkIn, checkOut, LocalDate.now(clock));
        if (problem != null) {
            presenter.presentInvalidRequest(problem);
            return;
        }
        presenter.presentAvailableRooms(checkIn, checkOut, StayPolicy.nights(checkIn, checkOut),
                rooms.findAll().stream()
                        .filter(room -> availability.isFree(room.getRoomNumber(), checkIn, checkOut))
                        .map(RoomResponse::from)
                        .toList());
    }
}
