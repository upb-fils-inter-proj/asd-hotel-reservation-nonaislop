package org.nightking.pricing;

public class StandardPricing implements PricingStrategy {
    @Override
    public double calculatePrice(double nightlyRate, int numberOfNights) {
        return nightlyRate * numberOfNights;
    }

    @Override
    public String getName() { return "Standard pricing"; }
}
