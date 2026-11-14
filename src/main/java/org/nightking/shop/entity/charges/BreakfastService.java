package org.nightking.shop.entity.charges;

public class BreakfastService extends ServiceDecorator {
    private final double pricePerNight;

    public BreakfastService(Chargeable wrappedCharge, double pricePerNight) {
        super(wrappedCharge);
        this.pricePerNight = pricePerNight;
    }

    @Override
    public double getCost() { return wrappedCharge.getCost() + pricePerNight; }

    @Override
    public String getDescription() {
        return wrappedCharge.getDescription() + " + Breakfast";
    }
}
