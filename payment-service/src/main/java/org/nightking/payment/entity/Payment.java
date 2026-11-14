package org.nightking.payment.entity;

/** A single attempt to charge a guest for a reservation. */
public class Payment {
    private final String reservationId;
    private final double amount;
    private PaymentStatus status = PaymentStatus.PENDING;

    public Payment(String reservationId, double amount) {
        this.reservationId = reservationId;
        this.amount = amount;
    }

    public String getReservationId() { return reservationId; }
    public double getAmount() { return amount; }
    public PaymentStatus getStatus() { return status; }

    public void approve() { status = PaymentStatus.APPROVED; }
    public void decline() { status = PaymentStatus.DECLINED; }
}
