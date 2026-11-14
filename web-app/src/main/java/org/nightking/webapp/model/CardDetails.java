package org.nightking.webapp.model;

/** Card details typed on the payment page. Sent to the hotel service for this one booking, never kept. */
public record CardDetails(String holderName, String number, int expiryMonth, int expiryYear, String cvc) {

    /** Never prints the full number or the security code. */
    @Override
    public String toString() {
        String digits = number == null ? "" : number.replaceAll("\\D", "");
        return "CardDetails[•••• " + (digits.length() >= 4 ? digits.substring(digits.length() - 4) : "") + "]";
    }
}
