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
    private String transactionId;
    private String reservationId;
    private BigDecimal amount;
    private String status;
    private String declineReason;
    private String cardBrand;
    private String cardLast4;
    private Instant processedAt;

    protected PaymentJpaEntity() {
    }

    PaymentJpaEntity(String transactionId, String reservationId, BigDecimal amount, String cardBrand, String cardLast4) {
        this.transactionId = transactionId;
        this.reservationId = reservationId;
        this.amount = amount;
        this.cardBrand = cardBrand;
        this.cardLast4 = cardLast4;
    }

    void update(String status, String declineReason, Instant processedAt) {
        this.status = status;
        this.declineReason = declineReason;
        this.processedAt = processedAt;
    }

    public String getTransactionId() { return transactionId; }
    public String getReservationId() { return reservationId; }
    public BigDecimal getAmount() { return amount; }
    public String getStatus() { return status; }
    public String getDeclineReason() { return declineReason; }
    public String getCardBrand() { return cardBrand; }
    public String getCardLast4() { return cardLast4; }
}
