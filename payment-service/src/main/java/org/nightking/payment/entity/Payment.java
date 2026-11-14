package org.nightking.payment.entity;

/** One attempt to charge a guest for a reservation, and what happened to it. */
public class Payment {
    private final String transactionId;
    private final String reservationId;
    private final double amount;
    private final String cardBrand;
    private final String cardLastFour;
    private PaymentStatus status;
    private String declineReason;

    public Payment(String transactionId, String reservationId, double amount, String cardBrand, String cardLastFour) {
        this(transactionId, reservationId, amount, cardBrand, cardLastFour, PaymentStatus.PENDING, null);
    }

    /** Rebuilds a stored payment. */
    public Payment(String transactionId, String reservationId, double amount, String cardBrand, String cardLastFour,
                   PaymentStatus status, String declineReason) {
        this.transactionId = transactionId;
        this.reservationId = reservationId;
        this.amount = amount;
        this.cardBrand = cardBrand;
        this.cardLastFour = cardLastFour;
        this.status = status;
        this.declineReason = declineReason;
    }

    public String getTransactionId() { return transactionId; }
    public String getReservationId() { return reservationId; }
    public double getAmount() { return amount; }
    public String getCardBrand() { return cardBrand; }
    public String getCardLastFour() { return cardLastFour; }
    public PaymentStatus getStatus() { return status; }
    public String getDeclineReason() { return declineReason; }

    public void approve() {
        status = PaymentStatus.APPROVED;
        declineReason = null;
    }

    public void decline(String reason) {
        status = PaymentStatus.DECLINED;
        declineReason = reason;
    }

    /** Only an approved payment can be refunded, and only once. */
    public boolean canBeRefunded() {
        return status == PaymentStatus.APPROVED;
    }

    public void markRefunded() {
        if (!canBeRefunded()) {
            throw new IllegalStateException("Payment " + transactionId + " is " + status + " and cannot be refunded.");
        }
        status = PaymentStatus.REFUNDED;
    }
}
