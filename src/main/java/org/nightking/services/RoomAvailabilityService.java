package org.nightking.services;

import org.nightking.model.Room;
import org.nightking.model.RoomStatus;

public class RoomAvailabilityService {
    public boolean isAvailable(Room room) {
        return room.getStatus() == RoomStatus.AVAILABLE;
    }
}
