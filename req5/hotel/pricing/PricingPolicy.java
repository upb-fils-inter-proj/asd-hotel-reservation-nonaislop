package hotel.pricing;

public class PricingPolicy {
    private PricingStrategy strategy;

    public PricingPolicy(PricingStrategy strategy) {
        this.strategy = strategy;
    }

    public void setStrategy(PricingStrategy strategy) {
        this.strategy = strategy;
    }

    public double calculate(double nightlyRate, int nights) {
        double total = strategy.calculatePrice(nightlyRate, nights);
        System.out.println("Pricing strategy: " + strategy.getName());
        return total;
    }
}
