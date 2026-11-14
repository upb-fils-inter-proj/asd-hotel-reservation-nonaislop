package org.nightking.hotelmanagement.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReservationJpaRepository extends JpaRepository<ReservationJpaEntity, String> {
    List<ReservationJpaEntity> findAllByOrderByCheckInDescIdDesc();
    List<ReservationJpaEntity> findByGuest_IdOrderByCheckInDescIdDesc(String guestId);
    List<ReservationJpaEntity> findByRoom_RoomNumber(String roomNumber);

    @Query(value = "SELECT nextval('reservation_number_seq')", nativeQuery = true)
    long nextReservationNumber();
}
