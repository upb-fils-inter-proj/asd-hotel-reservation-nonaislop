package org.nightking.charges;

public class SpaPackage extends ServiceDecorator {
    private final double packagePrice;

    public SpaPackage(Chargeable wrappedCharge, double packagePrice) {
        super(wrappedCharge);
        this.packagePrice = packagePrice;
    }

    @Override
    public double getCost() { return wrappedCharge.getCost() + packagePrice; }

    @Override
    public String getDescription() {
        return wrappedCharge.getDescription() + " + Spa package";
    }
}
