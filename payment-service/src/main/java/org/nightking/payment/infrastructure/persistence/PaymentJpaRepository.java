package org.nightking.payment.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentJpaRepository extends JpaRepository<PaymentJpaEntity, Long> {
    List<PaymentJpaEntity> findByReservationId(String reservationId);
    Optional<PaymentJpaEntity> findByTransactionId(String transactionId);
}
