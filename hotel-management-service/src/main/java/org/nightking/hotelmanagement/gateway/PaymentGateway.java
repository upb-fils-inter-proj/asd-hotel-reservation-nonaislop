package org.nightking.hotelmanagement.gateway;

/** What hotel management needs from the payment service: charge a card, and refund a payment. */
public interface PaymentGateway {
    ChargeResult charge(String reservationId, double amount, Card card);

    void refund(String paymentReference);

    /** Card details passed straight through to the payment service; never stored by the hotel. */
    record Card(String holderName, String number, int expiryMonth, int expiryYear, String cvc) {
        @Override
        public String toString() {
            String digits = number == null ? "" : number.replaceAll("\\D", "");
            return "Card[•••• " + (digits.length() >= 4 ? digits.substring(digits.length() - 4) : "") + "]";
        }
    }

    /** approved, plus the payment's reference and a display text such as "Visa •••• 4242"; or the decline reason. */
    record ChargeResult(boolean approved, String paymentReference, String paidWith, String declineReason) {}
}
