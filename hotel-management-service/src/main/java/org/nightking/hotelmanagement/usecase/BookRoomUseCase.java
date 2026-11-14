package org.nightking.hotelmanagement.usecase;

import org.nightking.hotelmanagement.model.BookRoomRequest;

/** Books a room: prices the stay with the shop, charges the guest through payment, then confirms. */
public interface BookRoomUseCase {
    void execute(BookRoomRequest request, ReservationPresenter presenter);
}
