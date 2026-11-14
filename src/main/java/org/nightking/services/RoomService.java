package org.nightking.services;

import org.nightking.model.Room;
import org.nightking.model.RoomStatus;

public class RoomService {
    public void markOccupied(Room room) {
        room.updateStatus(RoomStatus.OCCUPIED);
    }

    public void markAvailable(Room room) {
        room.updateStatus(RoomStatus.AVAILABLE);
    }
}
