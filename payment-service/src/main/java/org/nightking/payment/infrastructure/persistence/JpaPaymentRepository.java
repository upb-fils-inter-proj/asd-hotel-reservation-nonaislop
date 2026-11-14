package org.nightking.payment.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import org.nightking.payment.entity.Payment;
import org.nightking.payment.gateway.PaymentRepository;

/** PaymentRepository gateway backed by the payment database. */
public class JpaPaymentRepository implements PaymentRepository {
    private final PaymentJpaRepository payments;

    public JpaPaymentRepository(PaymentJpaRepository payments) {
        this.payments = payments;
    }

    @Override
    public void save(Payment payment) {
        payments.save(new PaymentJpaEntity(payment.getReservationId(), BigDecimal.valueOf(payment.getAmount()),
                payment.getStatus().name(), Instant.now()));
    }
}
