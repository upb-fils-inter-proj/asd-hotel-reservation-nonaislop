package org.nightking.hotelmanagement.infrastructure.persistence;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.nightking.hotelmanagement.entity.Room;
import org.nightking.hotelmanagement.entity.RoomStatus;
import org.nightking.hotelmanagement.gateway.RoomRepository;

/** RoomRepository gateway backed by the hotel database. */
public class JpaRoomRepository implements RoomRepository {
    private final RoomJpaRepository rooms;

    public JpaRoomRepository(RoomJpaRepository rooms) {
        this.rooms = rooms;
    }

    @Override
    public Optional<Room> findByNumber(String roomNumber) {
        return rooms.findById(roomNumber).map(JpaRoomRepository::toDomain);
    }

    @Override
    public List<Room> findAll() {
        return rooms.findAllByOrderByRoomNumber().stream().map(JpaRoomRepository::toDomain).toList();
    }

    @Override
    public void save(Room room) {
        rooms.save(new RoomJpaEntity(room.getRoomNumber(), room.getRoomType(),
                BigDecimal.valueOf(room.getNightlyRate()), room.getStatus().name()));
    }

    static Room toDomain(RoomJpaEntity entity) {
        return new Room(entity.getRoomNumber(), entity.getRoomType(), entity.getNightlyRate().doubleValue(),
                RoomStatus.valueOf(entity.getStatus()));
    }
}
