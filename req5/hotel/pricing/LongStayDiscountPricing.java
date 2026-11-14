package hotel.pricing;

public class LongStayDiscountPricing implements PricingStrategy {
    private final int minimumNights;
    private final double discountRate;

    public LongStayDiscountPricing(int minimumNights, double discountRate) {
        this.minimumNights = minimumNights;
        this.discountRate = discountRate;
    }

    @Override
    public double calculatePrice(double nightlyRate, int numberOfNights) {
        double total = nightlyRate * numberOfNights;
        return numberOfNights >= minimumNights ? total * (1.0 - discountRate) : total;
    }

    @Override
    public String getName() {
        return "Long-stay discount (" + (discountRate * 100) + "% from " + minimumNights + " nights)";
    }
}
