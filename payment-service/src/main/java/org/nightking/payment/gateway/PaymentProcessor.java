package org.nightking.payment.gateway;

public interface PaymentProcessor {
    boolean processPayment(String reservationId, double amount);
}
