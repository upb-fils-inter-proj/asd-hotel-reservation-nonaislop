package org.nightking.hotelmanagement.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationJpaRepository extends JpaRepository<ReservationJpaEntity, String> {
    List<ReservationJpaEntity> findAllByOrderByCheckInDescIdDesc();
    List<ReservationJpaEntity> findByGuest_IdOrderByCheckInDescIdDesc(String guestId);
    List<ReservationJpaEntity> findByRoom_RoomNumber(String roomNumber);

    @Query(value = "SELECT nextval('reservation_number_seq')", nativeQuery = true)
    long nextReservationNumber();

    // --- room_nights: one row per room per night; the primary key makes double bookings impossible

    @Query(value = "SELECT COUNT(*) FROM room_nights WHERE reservation_id = :reservationId", nativeQuery = true)
    long countHeldNights(@Param("reservationId") String reservationId);

    @Modifying
    @Query(value = "INSERT INTO room_nights (room_number, night, reservation_id) VALUES (:roomNumber, :night, :reservationId)",
            nativeQuery = true)
    void holdNight(@Param("roomNumber") String roomNumber, @Param("night") LocalDate night,
                   @Param("reservationId") String reservationId);

    @Modifying
    @Query(value = "DELETE FROM room_nights WHERE reservation_id = :reservationId", nativeQuery = true)
    void releaseNights(@Param("reservationId") String reservationId);
}
