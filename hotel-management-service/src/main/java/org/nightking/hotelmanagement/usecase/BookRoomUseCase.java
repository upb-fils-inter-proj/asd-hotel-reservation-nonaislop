package org.nightking.hotelmanagement.usecase;

import org.nightking.hotelmanagement.model.BookRoomRequest;

/**
 * Books a room for a registered guest: checks the dates and availability, prices the stay with the
 * shop, holds the room, charges the card through payment, then confirms. A declined card books nothing.
 */
public interface BookRoomUseCase {
    void execute(BookRoomRequest request, ReservationPresenter presenter);
}
