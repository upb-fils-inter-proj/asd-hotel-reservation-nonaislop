package org.nightking.hotelmanagement.presenter;

import java.util.List;
import org.nightking.hotelmanagement.model.RoomResponse;
import org.nightking.hotelmanagement.usecase.RoomListPresenter;
import org.springframework.http.ResponseEntity;

public class RoomListWebPresenter implements RoomListPresenter {
    private ResponseEntity<Object> response;

    @Override
    public void presentRooms(List<RoomResponse> rooms) {
        response = ResponseEntity.ok(rooms.stream().map(RoomViewModel::from).toList());
    }

    public ResponseEntity<Object> response() {
        if (response == null) {
            throw new IllegalStateException("The use case did not present a result.");
        }
        return response;
    }
}
