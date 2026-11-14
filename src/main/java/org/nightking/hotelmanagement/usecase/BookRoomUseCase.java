package org.nightking.hotelmanagement.usecase;

import java.util.Optional;
import org.nightking.hotelmanagement.model.BookRoomRequest;
import org.nightking.hotelmanagement.model.ReservationResponse;

/**
 * Books a room: prices the stay with the shop, charges the guest through payment, then confirms.
 * Empty if the booking was rejected (unknown room, room taken, invalid dates).
 */
public interface BookRoomUseCase {
    Optional<ReservationResponse> execute(BookRoomRequest request);
}
