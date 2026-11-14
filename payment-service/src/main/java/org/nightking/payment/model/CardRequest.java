package org.nightking.payment.model;

/** Card details as sent by the caller. */
public record CardRequest(String holderName, String number, int expiryMonth, int expiryYear, String cvc) {

    /** Never prints the full number or the security code. */
    @Override
    public String toString() {
        String digits = number == null ? "" : number.replaceAll("\\D", "");
        return "CardRequest[•••• " + (digits.length() >= 4 ? digits.substring(digits.length() - 4) : "") + "]";
    }
}
