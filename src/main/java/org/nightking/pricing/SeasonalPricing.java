package org.nightking.pricing;

public class SeasonalPricing implements PricingStrategy {
    private final double multiplier;

    public SeasonalPricing(double multiplier) {
        this.multiplier = multiplier;
    }

    @Override
    public double calculatePrice(double nightlyRate, int numberOfNights) {
        return nightlyRate * numberOfNights * multiplier;
    }

    @Override
    public String getName() { return "Seasonal pricing (x" + multiplier + ")"; }
}
