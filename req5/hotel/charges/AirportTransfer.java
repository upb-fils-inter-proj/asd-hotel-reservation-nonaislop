package hotel.charges;

public class AirportTransfer extends ServiceDecorator {
    private final double transferPrice;

    public AirportTransfer(Chargeable wrappedCharge, double transferPrice) {
        super(wrappedCharge);
        this.transferPrice = transferPrice;
    }

    @Override
    public double getCost() { return wrappedCharge.getCost() + transferPrice; }

    @Override
    public String getDescription() {
        return wrappedCharge.getDescription() + " + Airport transfer";
    }
}
