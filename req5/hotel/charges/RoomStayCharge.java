package hotel.charges;

public class RoomStayCharge implements Chargeable {
    private final double roomCost;

    public RoomStayCharge(double roomCost) {
        this.roomCost = roomCost;
    }

    @Override
    public double getCost() { return roomCost; }

    @Override
    public String getDescription() { return "Room stay"; }
}
