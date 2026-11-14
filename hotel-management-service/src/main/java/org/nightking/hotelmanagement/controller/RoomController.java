package org.nightking.hotelmanagement.controller;

import java.time.LocalDate;
import org.nightking.hotelmanagement.presenter.AvailabilityWebPresenter;
import org.nightking.hotelmanagement.presenter.RoomListWebPresenter;
import org.nightking.hotelmanagement.usecase.ListRoomsUseCase;
import org.nightking.hotelmanagement.usecase.SearchAvailableRoomsUseCase;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {
    private final ListRoomsUseCase listRooms;
    private final SearchAvailableRoomsUseCase searchAvailableRooms;

    public RoomController(ListRoomsUseCase listRooms, SearchAvailableRoomsUseCase searchAvailableRooms) {
        this.listRooms = listRooms;
        this.searchAvailableRooms = searchAvailableRooms;
    }

    @GetMapping
    public ResponseEntity<Object> list() {
        RoomListWebPresenter presenter = new RoomListWebPresenter();
        listRooms.execute(presenter);
        return presenter.response();
    }

    @GetMapping("/available")
    public ResponseEntity<Object> available(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut) {
        AvailabilityWebPresenter presenter = new AvailabilityWebPresenter();
        searchAvailableRooms.execute(checkIn, checkOut, presenter);
        return presenter.response();
    }
}
