package org.nightking.hotelmanagement.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "rooms")
public class RoomJpaEntity {
    @Id
    private String roomNumber;
    private String roomType;
    private BigDecimal nightlyRate;
    private String status;

    protected RoomJpaEntity() {
    }

    RoomJpaEntity(String roomNumber, String roomType, BigDecimal nightlyRate, String status) {
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.nightlyRate = nightlyRate;
        this.status = status;
    }

    String getRoomNumber() { return roomNumber; }
    String getRoomType() { return roomType; }
    BigDecimal getNightlyRate() { return nightlyRate; }
    String getStatus() { return status; }
}
