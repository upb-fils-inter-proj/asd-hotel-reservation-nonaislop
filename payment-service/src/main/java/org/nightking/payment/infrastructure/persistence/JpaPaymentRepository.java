package org.nightking.payment.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.nightking.payment.entity.Payment;
import org.nightking.payment.entity.PaymentStatus;
import org.nightking.payment.gateway.PaymentRepository;

/** PaymentRepository gateway backed by the payment database. Never stores full card numbers. */
public class JpaPaymentRepository implements PaymentRepository {
    private final PaymentJpaRepository payments;
    private final Clock clock;

    public JpaPaymentRepository(PaymentJpaRepository payments, Clock clock) {
        this.payments = payments;
        this.clock = clock;
    }

    @Override
    public void save(Payment payment) {
        PaymentJpaEntity entity = payments.findByTransactionId(payment.getTransactionId())
                .orElseGet(() -> new PaymentJpaEntity(payment.getTransactionId(), payment.getReservationId(),
                        BigDecimal.valueOf(payment.getAmount()), payment.getCardBrand(), payment.getCardLastFour()));
        entity.update(payment.getStatus().name(), payment.getDeclineReason(), Instant.now(clock));
        payments.save(entity);
    }

    @Override
    public Optional<Payment> findByTransactionId(String transactionId) {
        return payments.findByTransactionId(transactionId).map(e -> new Payment(e.getTransactionId(),
                e.getReservationId(), e.getAmount().doubleValue(), e.getCardBrand(), e.getCardLast4(),
                PaymentStatus.valueOf(e.getStatus()), e.getDeclineReason()));
    }
}
