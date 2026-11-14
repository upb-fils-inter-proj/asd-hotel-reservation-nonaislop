package org.nightking.hotelmanagement.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nightking.hotelmanagement.entity.Account;
import org.nightking.hotelmanagement.entity.AccountRole;
import org.nightking.hotelmanagement.entity.Reservation;
import org.nightking.hotelmanagement.entity.Room;
import org.nightking.hotelmanagement.entity.RoomStatus;
import org.nightking.hotelmanagement.entity.lifecycle.CheckedInState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

/** The JPA gateway adapters against the real schema (Flyway migrations on H2 in PostgreSQL mode). */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PersistenceAdaptersTest {

    @Autowired private RoomJpaRepository roomJpa;
    @Autowired private AccountJpaRepository accountJpa;
    @Autowired private ReservationJpaRepository reservationJpa;

    private JpaRoomRepository rooms;
    private JpaAccountRepository accounts;
    private JpaReservationRepository reservations;

    @BeforeEach
    void adapters() {
        rooms = new JpaRoomRepository(roomJpa);
        accounts = new JpaAccountRepository(accountJpa);
        reservations = new JpaReservationRepository(reservationJpa, roomJpa, accountJpa);
    }

    @Test
    void roomsAreSeededAndStatusIsSaved() {
        assertEquals(List.of("101", "204", "305"), rooms.findAll().stream().map(Room::getRoomNumber).toList());

        Room room = rooms.findByNumber("204").orElseThrow();
        room.updateStatus(RoomStatus.OCCUPIED);
        rooms.save(room);
        assertEquals(RoomStatus.OCCUPIED, rooms.findByNumber("204").orElseThrow().getStatus());
    }

    @Test
    void accountsAreFoundByUsername() {
        accounts.save(new Account("g-1", "alex", "hash", AccountRole.GUEST, "Alex Morgan", "alex@example.com", "+40"));
        Account loaded = accounts.findByUsername("alex").orElseThrow();
        assertEquals("g-1", loaded.getId());
        assertEquals(AccountRole.GUEST, loaded.getRole());
        assertTrue(accounts.findByUsername("nobody").isEmpty());
    }

    @Test
    void reservationsKeepTheirGuestRoomAndLifecycleState() {
        Account guest = new Account("g-1", "alex", "hash", AccountRole.GUEST, "Alex Morgan", "alex@example.com", "+40");
        accounts.save(guest);
        String id = reservations.nextId();
        assertEquals("RES-1000", id);
        assertEquals("RES-1001", reservations.nextId());

        Reservation reservation = new Reservation(id, guest.asGuest(), rooms.findByNumber("204").orElseThrow(),
                LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 13), 450.0);
        reservation.setState(new CheckedInState());
        reservations.save(reservation);

        Reservation loaded = reservations.findById(id).orElseThrow();
        assertEquals("CHECKED_IN", loaded.getState().getName());
        assertEquals("Alex Morgan", loaded.getGuest().getName());
        assertEquals("204", loaded.getRoom().getRoomNumber());
        assertEquals(450.0, loaded.getTotalPrice(), 1e-9);
        assertEquals(1, reservations.findByGuestId("g-1").size());
        assertEquals(1, reservations.findByRoomNumber("204").size());
        assertTrue(reservations.findByRoomNumber("101").isEmpty());
    }
}
