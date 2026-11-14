package org.nightking.hotelmanagement.model;

import org.nightking.hotelmanagement.entity.Room;

public record RoomResponse(String roomNumber, String roomType, double nightlyRate, String status) {

    public static RoomResponse from(Room room) {
        return new RoomResponse(room.getRoomNumber(), room.getRoomType(), room.getNightlyRate(),
                room.getStatus().name());
    }
}
