package org.nightking.payment.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.YearMonth;
import org.junit.jupiter.api.Test;

class PaymentCardTest {
    private static final YearMonth NOW = YearMonth.of(2026, 11);

    private static PaymentCard card(String number, int month, int year, String cvc) {
        return new PaymentCard("Alex Morgan", number, month, year, cvc);
    }

    @Test
    void acceptsAValidCardWrittenWithSpaces() {
        PaymentCard card = card("4242 4242 4242 4242", 12, 2028, "123");
        assertNull(card.validationProblem(NOW));
        assertEquals("Visa", card.brand());
        assertEquals("4242", card.lastFour());
    }

    @Test
    void rejectsNumbersThatFailTheLuhnCheck() {
        assertEquals("Card number is not valid.", card("4242 4242 4242 4241", 12, 2028, "123").validationProblem(NOW));
        assertEquals("Card number is not valid.", card("1234", 12, 2028, "123").validationProblem(NOW));
    }

    @Test
    void rejectsExpiredCardsButAcceptsTheCurrentMonth() {
        assertEquals("The card has expired.", card("4242424242424242", 10, 2026, "123").validationProblem(NOW));
        assertNull(card("4242424242424242", 11, 2026, "123").validationProblem(NOW));
        assertNull(card("4242424242424242", 11, 26, "123").validationProblem(NOW), "two-digit years are accepted");
    }

    @Test
    void rejectsBadMonthsCvcsAndMissingNames() {
        assertEquals("Expiry date is not valid.", card("4242424242424242", 13, 2028, "123").validationProblem(NOW));
        assertEquals("Security code must be 3 or 4 digits.", card("4242424242424242", 12, 2028, "12").validationProblem(NOW));
        assertEquals("Cardholder name is required.",
                new PaymentCard(" ", "4242424242424242", 12, 2028, "123").validationProblem(NOW));
    }

    @Test
    void recognisesBrands() {
        assertEquals("Mastercard", card("5555555555554444", 12, 2028, "123").brand());
        assertEquals("American Express", card("378282246310005", 12, 2028, "1234").brand());
    }

    @Test
    void neverPrintsTheFullNumberOrCvc() {
        String text = card("4242 4242 4242 4242", 12, 2028, "987").toString();
        assertEquals("Visa •••• 4242", text);
        assertFalse(text.contains("987"));
    }
}
