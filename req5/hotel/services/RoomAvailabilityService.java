package hotel.services;

import hotel.model.Room;
import hotel.model.RoomStatus;

public class RoomAvailabilityService {
    public boolean isAvailable(Room room) {
        return room.getStatus() == RoomStatus.AVAILABLE;
    }
}
