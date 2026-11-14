package org.nightking.hotelmanagement.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import org.nightking.hotelmanagement.entity.lifecycle.ConfirmedState;
import org.nightking.hotelmanagement.gateway.RoomUnavailableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The JPA gateway adapters against the real schema (Flyway migrations on H2 in PostgreSQL mode).
 * Each save runs in its own transaction, as in production, so database constraints are really hit.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PersistenceAdaptersTest {

    @Autowired private RoomJpaRepository roomJpa;
    @Autowired private AccountJpaRepository accountJpa;
    @Autowired private ReservationJpaRepository reservationJpa;
    @Autowired private PlatformTransactionManager transactionManager;

    private JpaRoomRepository rooms;
    private JpaAccountRepository accounts;
    private JpaReservationRepository reservations;
    private Account guest;

    @BeforeEach
    void adapters() {
        rooms = new JpaRoomRepository(roomJpa);
        accounts = new JpaAccountRepository(accountJpa);
        reservations = new JpaReservationRepository(reservationJpa, roomJpa, accountJpa, transactionManager);
        guest = new Account("g-1", "alex", "hash", AccountRole.GUEST, "Alex Morgan", "alex@example.com", "+40");
        accounts.save(guest);
    }

    private Reservation confirmed(String roomNumber, LocalDate in, LocalDate out) {
        Reservation reservation = new Reservation(reservations.nextId(), guest.asGuest(),
                rooms.findByNumber(roomNumber).orElseThrow(), in, out, 300.0);
        reservation.setState(new ConfirmedState());
        return reservation;
    }

    @Test
    void roomsAreSeededAndStatusIsSaved() {
        assertEquals(List.of("101", "204", "305"), rooms.findAll().stream().map(Room::getRoomNumber).toList());

        Room room = rooms.findByNumber("305").orElseThrow();
        room.updateStatus(RoomStatus.OCCUPIED);
        rooms.save(room);
        assertEquals(RoomStatus.OCCUPIED, rooms.findByNumber("305").orElseThrow().getStatus());
        room.updateStatus(RoomStatus.AVAILABLE);
        rooms.save(room);
    }

    @Test
    void accountsAreFoundByUsername() {
        Account loaded = accounts.findByUsername("alex").orElseThrow();
        assertEquals("g-1", loaded.getId());
        assertEquals(AccountRole.GUEST, loaded.getRole());
        assertTrue(accounts.findByUsername("nobody").isEmpty());
    }

    @Test
    void reservationsKeepTheirGuestRoomStateAndPayment() {
        Reservation reservation = new Reservation(reservations.nextId(), guest.asGuest(),
                rooms.findByNumber("204").orElseThrow(), LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 4), 450.0);
        reservation.setState(new CheckedInState());
        reservation.recordPayment("PAY-123", "Visa •••• 4242");
        reservations.save(reservation);

        Reservation loaded = reservations.findById(reservation.getId()).orElseThrow();
        assertEquals("CHECKED_IN", loaded.getState().getName());
        assertEquals("Alex Morgan", loaded.getGuest().getName());
        assertEquals("204", loaded.getRoom().getRoomNumber());
        assertEquals(450.0, loaded.getTotalPrice(), 1e-9);
        assertEquals("PAY-123", loaded.getPaymentReference());
        assertEquals("Visa •••• 4242", loaded.getPaidWith());
        assertTrue(reservations.findByGuestId("g-1").size() >= 1);
    }

    @Test
    void theDatabaseRefusesTwoHoldsOnTheSameNight() {
        Reservation first = confirmed("101", LocalDate.of(2027, 5, 10), LocalDate.of(2027, 5, 13));
        reservations.save(first);
        assertEquals(3, reservationJpa.countHeldNights(first.getId()));

        Reservation overlapping = confirmed("101", LocalDate.of(2027, 5, 12), LocalDate.of(2027, 5, 14));
        assertThrows(RoomUnavailableException.class, () -> reservations.save(overlapping));
        assertTrue(reservations.findById(overlapping.getId()).isEmpty(), "nothing of the refused booking is kept");

        Reservation backToBack = confirmed("101", LocalDate.of(2027, 5, 13), LocalDate.of(2027, 5, 15));
        assertDoesNotThrow(() -> reservations.save(backToBack));
    }

    @Test
    void cancellingReleasesTheNights() {
        Reservation first = confirmed("305", LocalDate.of(2027, 6, 1), LocalDate.of(2027, 6, 3));
        reservations.save(first);

        first.cancel();
        reservations.save(first);
        assertEquals(0, reservationJpa.countHeldNights(first.getId()));

        assertDoesNotThrow(() -> reservations.save(confirmed("305", LocalDate.of(2027, 6, 1), LocalDate.of(2027, 6, 3))));
    }
}
