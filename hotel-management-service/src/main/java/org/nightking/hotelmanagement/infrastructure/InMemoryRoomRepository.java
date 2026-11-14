package org.nightking.hotelmanagement.infrastructure;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.nightking.hotelmanagement.entity.Room;
import org.nightking.hotelmanagement.gateway.RoomRepository;

public class InMemoryRoomRepository implements RoomRepository {
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();

    @Override
    public Optional<Room> findByNumber(String roomNumber) {
        return Optional.ofNullable(rooms.get(roomNumber));
    }

    @Override
    public void save(Room room) {
        rooms.put(room.getRoomNumber(), room);
    }
}
