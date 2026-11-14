package org.nightking.payment.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments")
public class PaymentJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String reservationId;
    private BigDecimal amount;
    private String status;
    private Instant processedAt;

    protected PaymentJpaEntity() {
    }

    PaymentJpaEntity(String reservationId, BigDecimal amount, String status, Instant processedAt) {
        this.reservationId = reservationId;
        this.amount = amount;
        this.status = status;
        this.processedAt = processedAt;
    }

    public String getReservationId() { return reservationId; }
    public BigDecimal getAmount() { return amount; }
    public String getStatus() { return status; }
}
