package org.nightking.payment.gateway;

import org.nightking.payment.entity.Payment;

/** Keeps a record of every payment attempt. */
public interface PaymentRepository {
    void save(Payment payment);
}
