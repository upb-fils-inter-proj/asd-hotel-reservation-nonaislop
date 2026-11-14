package org.nightking.hotelmanagement.usecase;

import org.nightking.hotelmanagement.gateway.RoomRepository;
import org.nightking.hotelmanagement.model.RoomResponse;

public class ListRoomsInteractor implements ListRoomsUseCase {
    private final RoomRepository rooms;

    public ListRoomsInteractor(RoomRepository rooms) {
        this.rooms = rooms;
    }

    @Override
    public void execute(RoomListPresenter presenter) {
        presenter.presentRooms(rooms.findAll().stream().map(RoomResponse::from).toList());
    }
}
