package org.nightking.webapp.model;

import java.math.BigDecimal;

public record Room(String roomNumber, String roomType, BigDecimal nightlyRate, String currency, String status) {
}
