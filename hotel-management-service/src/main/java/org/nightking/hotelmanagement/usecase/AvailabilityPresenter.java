package org.nightking.hotelmanagement.usecase;

import java.time.LocalDate;
import java.util.List;
import org.nightking.hotelmanagement.model.RoomResponse;

public interface AvailabilityPresenter {
    void presentAvailableRooms(LocalDate checkIn, LocalDate checkOut, int nights, List<RoomResponse> rooms);
    void presentInvalidRequest(String message);
}
