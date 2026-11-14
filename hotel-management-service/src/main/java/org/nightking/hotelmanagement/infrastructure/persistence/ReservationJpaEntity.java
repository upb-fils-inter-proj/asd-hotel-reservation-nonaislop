package org.nightking.hotelmanagement.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "reservations")
public class ReservationJpaEntity {
    @Id
    private String id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "guest_id")
    private AccountJpaEntity guest;

    @ManyToOne(optional = false)
    @JoinColumn(name = "room_number")
    private RoomJpaEntity room;

    private LocalDate checkIn;
    private LocalDate checkOut;
    private BigDecimal totalPrice;
    private String state;
    private String paymentReference;
    private String paidWith;

    protected ReservationJpaEntity() {
    }

    ReservationJpaEntity(String id, AccountJpaEntity guest, RoomJpaEntity room, LocalDate checkIn,
                         LocalDate checkOut, BigDecimal totalPrice, String state,
                         String paymentReference, String paidWith) {
        this.id = id;
        this.guest = guest;
        this.room = room;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.totalPrice = totalPrice;
        this.state = state;
        this.paymentReference = paymentReference;
        this.paidWith = paidWith;
    }

    String getId() { return id; }
    AccountJpaEntity getGuest() { return guest; }
    RoomJpaEntity getRoom() { return room; }
    LocalDate getCheckIn() { return checkIn; }
    LocalDate getCheckOut() { return checkOut; }
    BigDecimal getTotalPrice() { return totalPrice; }
    String getState() { return state; }
    String getPaymentReference() { return paymentReference; }
    String getPaidWith() { return paidWith; }
}
