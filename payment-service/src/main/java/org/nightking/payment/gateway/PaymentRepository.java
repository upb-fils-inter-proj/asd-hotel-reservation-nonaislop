package org.nightking.payment.gateway;

import java.util.Optional;
import org.nightking.payment.entity.Payment;

/** Keeps a record of every payment attempt. */
public interface PaymentRepository {
    void save(Payment payment);
    Optional<Payment> findByTransactionId(String transactionId);
}
