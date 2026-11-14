package org.nightking.payment.model;

public record PaymentRequest(String reservationId, double amount) {
}
