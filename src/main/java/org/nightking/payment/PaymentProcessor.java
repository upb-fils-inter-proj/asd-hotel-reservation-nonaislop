package org.nightking.payment;

public interface PaymentProcessor {
    boolean processPayment(String reservationId, double amount);
}
