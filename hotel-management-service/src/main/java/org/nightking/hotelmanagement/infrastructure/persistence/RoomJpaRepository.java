package org.nightking.hotelmanagement.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomJpaRepository extends JpaRepository<RoomJpaEntity, String> {
    List<RoomJpaEntity> findAllByOrderByRoomNumber();
}
