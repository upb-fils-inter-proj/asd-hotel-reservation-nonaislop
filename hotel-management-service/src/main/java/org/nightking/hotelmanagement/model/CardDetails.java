package org.nightking.hotelmanagement.model;

/** Card details for paying a booking. Passed to the payment service and never stored here. */
public record CardDetails(String holderName, String number, int expiryMonth, int expiryYear, String cvc) {

    /** Never prints the full number or the security code. */
    @Override
    public String toString() {
        String digits = number == null ? "" : number.replaceAll("\\D", "");
        return "CardDetails[•••• " + (digits.length() >= 4 ? digits.substring(digits.length() - 4) : "") + "]";
    }
}
