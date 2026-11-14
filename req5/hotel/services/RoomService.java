package hotel.services;

import hotel.model.Room;
import hotel.model.RoomStatus;

public class RoomService {
    public void markOccupied(Room room) {
        room.updateStatus(RoomStatus.OCCUPIED);
    }

    public void markAvailable(Room room) {
        room.updateStatus(RoomStatus.AVAILABLE);
    }
}
