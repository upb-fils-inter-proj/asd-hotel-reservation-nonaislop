package org.nightking.hotelmanagement.usecase;

import java.util.List;
import org.nightking.hotelmanagement.model.RoomResponse;

public interface RoomListPresenter {
    void presentRooms(List<RoomResponse> rooms);
}
