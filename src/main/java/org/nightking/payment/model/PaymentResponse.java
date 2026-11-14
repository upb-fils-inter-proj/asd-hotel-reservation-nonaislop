package org.nightking.payment.model;

public record PaymentResponse(String reservationId, double amount, boolean approved) {
}
