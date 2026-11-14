package org.nightking.payment.infrastructure;

/**
 * Simulated third-party card network ("PayFlow") with its own, incompatible API: amounts in cents,
 * expiry as "MM/YY", and ISO-8583 style two-digit response codes. Test card numbers decide the
 * outcome, like a payment provider's sandbox:
 *
 * <ul>
 *   <li>4000 0000 0000 0002 — declined (do not honour)</li>
 *   <li>4000 0000 0000 9995 — insufficient funds</li>
 *   <li>4000 0000 0000 0069 — expired card</li>
 *   <li>any other valid number, e.g. 4242 4242 4242 4242 — approved</li>
 * </ul>
 */
public class PayFlowGateway {
    public static final String APPROVED = "00";
    public static final String DO_NOT_HONOUR = "05";
    public static final String INVALID_AMOUNT = "13";
    public static final String INSUFFICIENT_FUNDS = "51";
    public static final String EXPIRED_CARD = "54";

    public record PayFlowResponse(String responseCode, String message) {
        public boolean isApproved() { return APPROVED.equals(responseCode); }
    }

    public PayFlowResponse makeCharge(String bookingReference, String merchantReference, int amountInCents,
                                      String pan, String expiry, String cvc) {
        System.out.println("[PayFlow] Charging " + amountInCents + " cents for booking " + bookingReference
                + " (" + merchantReference + ")");
        if (amountInCents <= 0) return new PayFlowResponse(INVALID_AMOUNT, "Invalid amount");
        return switch (pan) {
            case "4000000000000002" -> new PayFlowResponse(DO_NOT_HONOUR, "Do not honour");
            case "4000000000009995" -> new PayFlowResponse(INSUFFICIENT_FUNDS, "Insufficient funds");
            case "4000000000000069" -> new PayFlowResponse(EXPIRED_CARD, "Expired card");
            default -> new PayFlowResponse(APPROVED, "Approved");
        };
    }

    public PayFlowResponse refundCharge(String merchantReference, int amountInCents) {
        System.out.println("[PayFlow] Refunding " + amountInCents + " cents for " + merchantReference);
        return amountInCents > 0
                ? new PayFlowResponse(APPROVED, "Refunded")
                : new PayFlowResponse(INVALID_AMOUNT, "Invalid amount");
    }
}
