package org.nightking.pricing;

public interface PricingStrategy {
    double calculatePrice(double nightlyRate, int numberOfNights);
    String getName();
}
