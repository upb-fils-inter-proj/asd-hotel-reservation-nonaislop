package org.nightking.payment;

public class PaymentService {
    private final PaymentProcessor processor;

    public PaymentService(PaymentProcessor processor) {
        this.processor = processor;
    }

    public boolean pay(String reservationId, double amount) {
        System.out.printf("Processing payment: EUR %.2f%n", amount);
        return processor.processPayment(reservationId, amount);
    }
}
