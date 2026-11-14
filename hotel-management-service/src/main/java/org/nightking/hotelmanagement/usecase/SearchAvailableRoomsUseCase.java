package org.nightking.hotelmanagement.usecase;

import java.time.LocalDate;

/** Lists the rooms that are free for every night between check-in and check-out. */
public interface SearchAvailableRoomsUseCase {
    void execute(LocalDate checkIn, LocalDate checkOut, AvailabilityPresenter presenter);
}
