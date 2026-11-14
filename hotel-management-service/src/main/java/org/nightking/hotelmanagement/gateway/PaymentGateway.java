package org.nightking.hotelmanagement.gateway;

/** What hotel management needs from the payment service: charge the guest. Returns true if approved. */
public interface PaymentGateway {
    boolean pay(String reservationId, double amount);
}
