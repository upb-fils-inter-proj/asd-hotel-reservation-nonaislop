package org.nightking.hotelmanagement.gateway;

import java.util.Optional;
import org.nightking.hotelmanagement.entity.Room;

public interface RoomRepository {
    Optional<Room> findByNumber(String roomNumber);
    void save(Room room);
}
