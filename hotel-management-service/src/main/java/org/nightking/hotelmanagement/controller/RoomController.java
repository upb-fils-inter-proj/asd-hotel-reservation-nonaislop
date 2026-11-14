package org.nightking.hotelmanagement.controller;

import org.nightking.hotelmanagement.presenter.RoomListWebPresenter;
import org.nightking.hotelmanagement.usecase.ListRoomsUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {
    private final ListRoomsUseCase listRooms;

    public RoomController(ListRoomsUseCase listRooms) {
        this.listRooms = listRooms;
    }

    @GetMapping
    public ResponseEntity<Object> list() {
        RoomListWebPresenter presenter = new RoomListWebPresenter();
        listRooms.execute(presenter);
        return presenter.response();
    }
}
