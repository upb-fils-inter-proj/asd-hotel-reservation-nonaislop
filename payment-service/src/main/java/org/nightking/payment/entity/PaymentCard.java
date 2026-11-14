package org.nightking.payment.entity;

import java.time.YearMonth;

/**
 * The card used for one charge. Card numbers and security codes are only held in memory for the
 * charge itself; a {@link Payment} keeps just the brand and the last four digits.
 */
public final class PaymentCard {
    private final String holderName;
    private final String number;
    private final int expiryMonth;
    private final int expiryYear;
    private final String cvc;

    public PaymentCard(String holderName, String number, int expiryMonth, int expiryYear, String cvc) {
        this.holderName = holderName == null ? "" : holderName.trim();
        this.number = number == null ? "" : number.replaceAll("[\\s-]", "");
        this.expiryMonth = expiryMonth;
        this.expiryYear = expiryYear < 100 ? 2000 + expiryYear : expiryYear;
        this.cvc = cvc == null ? "" : cvc.trim();
    }

    /** What is wrong with the card before it reaches the card network, or null if it looks valid. */
    public String validationProblem(YearMonth currentMonth) {
        if (holderName.isBlank()) return "Cardholder name is required.";
        if (!number.matches("\\d{12,19}") || !passesLuhnCheck()) return "Card number is not valid.";
        if (expiryMonth < 1 || expiryMonth > 12 || expiryYear > currentMonth.getYear() + 20) {
            return "Expiry date is not valid.";
        }
        if (YearMonth.of(expiryYear, expiryMonth).isBefore(currentMonth)) return "The card has expired.";
        if (!cvc.matches("\\d{3,4}")) return "Security code must be 3 or 4 digits.";
        return null;
    }

    /** The standard checksum that every real card number satisfies. */
    boolean passesLuhnCheck() {
        int sum = 0;
        boolean doubleIt = false;
        for (int i = number.length() - 1; i >= 0; i--) {
            int digit = number.charAt(i) - '0';
            if (doubleIt) {
                digit *= 2;
                if (digit > 9) digit -= 9;
            }
            sum += digit;
            doubleIt = !doubleIt;
        }
        return sum % 10 == 0;
    }

    public String brand() {
        if (number.startsWith("4")) return "Visa";
        if (number.matches("5[1-5].*") || number.matches("2(2[2-9]|[3-6]\\d|7[01]|720).*")) return "Mastercard";
        if (number.startsWith("34") || number.startsWith("37")) return "American Express";
        return "Card";
    }

    public String lastFour() {
        return number.length() >= 4 ? number.substring(number.length() - 4) : number;
    }

    public String holderName() { return holderName; }
    public String number() { return number; }
    public int expiryMonth() { return expiryMonth; }
    public int expiryYear() { return expiryYear; }
    public String cvc() { return cvc; }

    /** Never prints the full number or the security code. */
    @Override
    public String toString() {
        return brand() + " •••• " + lastFour();
    }
}
