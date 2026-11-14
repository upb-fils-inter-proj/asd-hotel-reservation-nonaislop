package org.nightking.hotelmanagement.gateway;

import java.util.List;
import java.util.Optional;
import org.nightking.hotelmanagement.entity.Room;

public interface RoomRepository {
    Optional<Room> findByNumber(String roomNumber);
    List<Room> findAll();
    void save(Room room);
}
